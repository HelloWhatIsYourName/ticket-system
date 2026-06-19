# Auth RBAC Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. When a step is completed, append `⭐` to that checked line.

**Goal:** Build the backend authentication and RBAC foundation required by the first-version project plan: login, JWT authentication, permission loading, menu summary, protected ping endpoints, and audit logging.

**Architecture:** Keep authentication concerns in `auth`, request security in `security`, and RBAC persistence in `system`. JWTs contain only stable identity claims (`userId`, `username`, `tokenVersion`, expiry); every authenticated request reloads current user status and authorities from Oracle so later Redis permission caching can be added without changing controller contracts. RBAC tables preserve future expansion through `department_id`, `data_scope`, `token_version`, `sys_menu`, and `audit_log`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Security 6, MyBatis XML, Flyway, Oracle 23ai Free, BCrypt, JJWT, JUnit 5, AssertJ, MockMvc.

---

## File Structure

Create:

- `backend/src/main/resources/db/migration/V2__auth_rbac.sql`
  - RBAC tables, indexes, seed roles, permissions, menus, demo users, and role-permission relations.
- `backend/src/main/java/com/example/aiticket/config/JwtProperties.java`
  - Binds `security.jwt.*`.
- `backend/src/main/java/com/example/aiticket/system/UserAccount.java`
  - User account projection for authentication and token validation.
- `backend/src/main/java/com/example/aiticket/system/MenuSummary.java`
  - Menu DTO returned by `/api/auth/me`.
- `backend/src/main/java/com/example/aiticket/system/UserAuthoritySnapshot.java`
  - Aggregated user, roles, permissions, and menus.
- `backend/src/main/java/com/example/aiticket/system/UserSecurityMapper.java`
  - MyBatis mapper interface for auth/RBAC queries.
- `backend/src/main/resources/mapper/UserSecurityMapper.xml`
  - Oracle SQL for user lookup, roles, permissions, visible menus, last login update, and audit insertion.
- `backend/src/main/java/com/example/aiticket/system/AuditLogService.java`
  - Writes authentication audit records.
- `backend/src/main/java/com/example/aiticket/system/UserAuthorityService.java`
  - Loads and aggregates current user authority snapshot.
- `backend/src/main/java/com/example/aiticket/security/JwtProperties.java`
  - Not used; keep all configuration in `config/JwtProperties.java` to avoid duplicate binding.
- `backend/src/main/java/com/example/aiticket/security/JwtService.java`
  - Issues and parses JWTs.
- `backend/src/main/java/com/example/aiticket/security/JwtAuthenticationFilter.java`
  - Parses Bearer token and sets Spring Security authentication.
- `backend/src/main/java/com/example/aiticket/security/SecurityConfig.java`
  - Security filter chain, method security, password encoder, exception responses.
- `backend/src/main/java/com/example/aiticket/security/AuthenticatedUser.java`
  - Principal used by Spring Security.
- `backend/src/main/java/com/example/aiticket/auth/AuthController.java`
  - `/api/auth/login`, `/api/auth/me`, `/api/auth/ping`.
- `backend/src/main/java/com/example/aiticket/auth/AuthService.java`
  - Login orchestration.
- `backend/src/main/java/com/example/aiticket/auth/LoginRequest.java`
  - Login request DTO.
- `backend/src/main/java/com/example/aiticket/auth/LoginResponse.java`
  - Login response DTO.
- `backend/src/main/java/com/example/aiticket/auth/CurrentUserResponse.java`
  - Current user DTO.
- `backend/src/main/java/com/example/aiticket/admin/AdminPingController.java`
  - `/api/admin/ping` protected by method-level permission.
- `backend/src/test/java/com/example/aiticket/config/JwtPropertiesTest.java`
  - YAML binding test for JWT settings.
- `backend/src/test/java/com/example/aiticket/security/JwtServiceTest.java`
  - Token generation, parsing, and expiry.
- `backend/src/test/java/com/example/aiticket/system/UserAuthorityServiceTest.java`
  - Role, permission, and menu aggregation behavior using mocked mapper.
- `backend/src/test/java/com/example/aiticket/auth/AuthServiceTest.java`
  - Login success/failure behavior using mocked collaborators.

Modify:

- `backend/pom.xml`
  - Add Spring Security and JJWT dependencies.
- `backend/src/main/resources/application.yml`
  - Add `security.jwt.secret` and `security.jwt.expires-in-seconds`.
- `backend/src/main/java/com/example/aiticket/AiTicketApplication.java`
  - Enable `JwtProperties`.
- `backend/src/main/java/com/example/aiticket/common/api/ApiResponse.java`
  - Add `fail(String message)` if needed for security error responses.

Do not implement in this plan:

- User/role/permission CRUD.
- Frontend login page or dynamic menu rendering.
- Redis permission cache.
- Department model or data-scope SQL filtering.
- Login rate limiting.

---

## Task 1: Add Security Dependencies And JWT Configuration

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/example/aiticket/AiTicketApplication.java`
- Create: `backend/src/main/java/com/example/aiticket/config/JwtProperties.java`
- Create: `backend/src/test/java/com/example/aiticket/config/JwtPropertiesTest.java`

- [x] **Step 1: Write JWT configuration binding test** ⭐

Create `backend/src/test/java/com/example/aiticket/config/JwtPropertiesTest.java`:

```java
package com.example.aiticket.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {
    @Test
    void bindsJwtSettings() throws Exception {
        String yaml = """
                security:
                  jwt:
                    secret: local-dev-secret-with-at-least-32-characters
                    expires-in-seconds: 7200
                """;

        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        sources.addFirst(loader.load("test", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8))).getFirst());

        JwtProperties properties = Binder.get(environment)
                .bind("security.jwt", Bindable.of(JwtProperties.class))
                .orElseThrow(() -> new IllegalStateException("jwt properties did not bind"));

        assertThat(properties.getSecret()).isEqualTo("local-dev-secret-with-at-least-32-characters");
        assertThat(properties.getExpiresInSeconds()).isEqualTo(7200);
    }
}
```

- [x] **Step 2: Run the focused test and confirm it fails** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=JwtPropertiesTest test
```

Expected:

```text
cannot find symbol
  symbol:   class JwtProperties
```

- [x] **Step 3: Add dependencies and configuration class** ⭐

Add dependencies to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

Create `backend/src/main/java/com/example/aiticket/config/JwtProperties.java`:

```java
package com.example.aiticket.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    @NotBlank
    private String secret = "dev-only-change-me-to-a-long-random-secret";

    @Min(60)
    private long expiresInSeconds = 7200;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
```

Modify `backend/src/main/java/com/example/aiticket/AiTicketApplication.java`:

```java
@EnableConfigurationProperties({AiProviderProperties.class, JwtProperties.class})
```

Add to `backend/src/main/resources/application.yml`:

```yaml
security:
  jwt:
    secret: ${APP_JWT_SECRET:dev-only-change-me-to-a-long-random-secret}
    expires-in-seconds: ${APP_JWT_EXPIRES_IN_SECONDS:7200}
```

- [x] **Step 4: Run focused test** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=JwtPropertiesTest test
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 5: Commit** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/example/aiticket/AiTicketApplication.java backend/src/main/java/com/example/aiticket/config/JwtProperties.java backend/src/test/java/com/example/aiticket/config/JwtPropertiesTest.java docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add jwt security configuration"
```

---

## Task 2: Add RBAC Schema And Seed Data

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__auth_rbac.sql`

- [x] **Step 1: Add Flyway migration** ⭐

Create `backend/src/main/resources/db/migration/V2__auth_rbac.sql` with these tables:

```sql
CREATE TABLE sys_user (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR2(64) NOT NULL,
    password_hash VARCHAR2(100) NOT NULL,
    display_name VARCHAR2(100) NOT NULL,
    email VARCHAR2(120),
    phone VARCHAR2(32),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    department_id NUMBER(19),
    token_version NUMBER(10) DEFAULT 0 NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE sys_role (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    role_code VARCHAR2(64) NOT NULL,
    role_name VARCHAR2(100) NOT NULL,
    data_scope VARCHAR2(30) DEFAULT 'SELF' NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_sys_role_code UNIQUE (role_code),
    CONSTRAINT ck_sys_role_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_role_data_scope CHECK (data_scope IN ('ALL', 'DEPARTMENT', 'SELF'))
);

CREATE TABLE sys_permission (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    permission_code VARCHAR2(100) NOT NULL,
    permission_name VARCHAR2(120) NOT NULL,
    module VARCHAR2(50) NOT NULL,
    description VARCHAR2(255),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_sys_permission_code UNIQUE (permission_code),
    CONSTRAINT ck_sys_permission_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE sys_menu (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    parent_id NUMBER(19),
    menu_code VARCHAR2(80) NOT NULL,
    menu_name VARCHAR2(100) NOT NULL,
    route_path VARCHAR2(200) NOT NULL,
    component VARCHAR2(200),
    permission_code VARCHAR2(100),
    icon VARCHAR2(80),
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    visible NUMBER(1) DEFAULT 1 NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_sys_menu_code UNIQUE (menu_code),
    CONSTRAINT ck_sys_menu_visible CHECK (visible IN (0, 1)),
    CONSTRAINT ck_sys_menu_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE sys_user_role (
    user_id NUMBER(19) NOT NULL,
    role_id NUMBER(19) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE sys_role_permission (
    role_id NUMBER(19) NOT NULL,
    permission_id NUMBER(19) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_sys_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
);

CREATE TABLE audit_log (
    id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    actor_user_id NUMBER(19),
    actor_username VARCHAR2(64),
    action VARCHAR2(80) NOT NULL,
    target_type VARCHAR2(80) NOT NULL,
    target_id VARCHAR2(80),
    result VARCHAR2(20) NOT NULL,
    message VARCHAR2(500),
    ip_address VARCHAR2(64),
    user_agent VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);
```

Seed roles, permissions, menus, and users. Use this BCrypt hash for local demo password `Admin_123456`:

```sql
INSERT INTO sys_role (id, role_code, role_name, data_scope, sort_order) VALUES (1, 'SUPER_ADMIN', '超级管理员', 'ALL', 1);
INSERT INTO sys_role (id, role_code, role_name, data_scope, sort_order) VALUES (2, 'ADMIN', '管理员', 'ALL', 2);
INSERT INTO sys_role (id, role_code, role_name, data_scope, sort_order) VALUES (3, 'AGENT', '坐席或工程师', 'SELF', 3);
INSERT INTO sys_role (id, role_code, role_name, data_scope, sort_order) VALUES (4, 'USER', '普通用户', 'SELF', 4);
```

Generate BCrypt hashes locally before inserting demo users:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -q -DskipTests exec:java -Dexec.mainClass=org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
```

If the direct encoder command is not available, use a temporary Java test to print hashes, then delete it before committing. Do not store plaintext passwords in the migration.

- [x] **Step 2: Verify migration with Oracle** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

Expected:

```text
Migrating schema "AI_TICKET" to version "2 - auth rbac"
Successfully applied 1 migration
```

- [x] **Step 3: Commit** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/resources/db/migration/V2__auth_rbac.sql docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add auth rbac schema"
```

---

## Task 3: Add RBAC Query Layer

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/system/UserAccount.java`
- Create: `backend/src/main/java/com/example/aiticket/system/MenuSummary.java`
- Create: `backend/src/main/java/com/example/aiticket/system/UserAuthoritySnapshot.java`
- Create: `backend/src/main/java/com/example/aiticket/system/UserSecurityMapper.java`
- Create: `backend/src/main/resources/mapper/UserSecurityMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/system/UserAuthorityService.java`
- Create: `backend/src/main/java/com/example/aiticket/system/AuditLogService.java`
- Create: `backend/src/test/java/com/example/aiticket/system/UserAuthorityServiceTest.java`

- [x] **Step 1: Write authority aggregation test** ⭐

Create `backend/src/test/java/com/example/aiticket/system/UserAuthorityServiceTest.java`:

```java
package com.example.aiticket.system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuthorityServiceTest {
    @Test
    void loadsSnapshotWithDistinctRolesPermissionsAndMenus() {
        UserSecurityMapper mapper = mock(UserSecurityMapper.class);
        UserAuthorityService service = new UserAuthorityService(mapper);
        UserAccount account = new UserAccount(1L, "admin", "$hash", "系统管理员", "ACTIVE", 0);

        when(mapper.findActiveUserByUsername("admin")).thenReturn(Optional.of(account));
        when(mapper.findRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN", "ADMIN"));
        when(mapper.findPermissionCodesByUserId(1L)).thenReturn(List.of("system:user:manage", "dashboard:view", "dashboard:view"));
        when(mapper.findVisibleMenusByUserId(1L)).thenReturn(List.of(
                new MenuSummary("users", "用户权限管理", "/admin/users", "User"),
                new MenuSummary("dashboard", "统计看板", "/admin/dashboard", "Chart")
        ));

        UserAuthoritySnapshot snapshot = service.loadByUsername("admin").orElseThrow();

        assertThat(snapshot.user()).isEqualTo(account);
        assertThat(snapshot.roles()).containsExactly("ADMIN");
        assertThat(snapshot.permissions()).containsExactly("dashboard:view", "system:user:manage");
        assertThat(snapshot.menus()).extracting(MenuSummary::code).containsExactly("users", "dashboard");
    }
}
```

- [x] **Step 2: Implement records, mapper, and service** ⭐

Create `UserAccount`:

```java
package com.example.aiticket.system;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String status,
        int tokenVersion
) {
    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
```

Create `MenuSummary`:

```java
package com.example.aiticket.system;

public record MenuSummary(String code, String name, String path, String icon) {
}
```

Create `UserAuthoritySnapshot`:

```java
package com.example.aiticket.system;

import java.util.List;

public record UserAuthoritySnapshot(
        UserAccount user,
        List<String> roles,
        List<String> permissions,
        List<MenuSummary> menus
) {
}
```

Create `UserSecurityMapper`:

```java
package com.example.aiticket.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserSecurityMapper {
    Optional<UserAccount> findActiveUserByUsername(@Param("username") String username);
    Optional<UserAccount> findActiveUserById(@Param("userId") Long userId);
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);
    List<MenuSummary> findVisibleMenusByUserId(@Param("userId") Long userId);
    void updateLastLoginAt(@Param("userId") Long userId);
    void insertAuditLog(@Param("actorUserId") Long actorUserId, @Param("actorUsername") String actorUsername,
                        @Param("action") String action, @Param("targetType") String targetType,
                        @Param("targetId") String targetId, @Param("result") String result,
                        @Param("message") String message, @Param("ipAddress") String ipAddress,
                        @Param("userAgent") String userAgent);
}
```

Create `UserAuthorityService`:

```java
package com.example.aiticket.system;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class UserAuthorityService {
    private final UserSecurityMapper mapper;

    public UserAuthorityService(UserSecurityMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserAuthoritySnapshot> loadByUsername(String username) {
        return mapper.findActiveUserByUsername(username).map(this::snapshot);
    }

    public Optional<UserAuthoritySnapshot> loadByUserId(Long userId) {
        return mapper.findActiveUserById(userId).map(this::snapshot);
    }

    private UserAuthoritySnapshot snapshot(UserAccount user) {
        List<String> roles = distinctSorted(mapper.findRoleCodesByUserId(user.id()));
        List<String> permissions = distinctSorted(mapper.findPermissionCodesByUserId(user.id()));
        List<MenuSummary> menus = List.copyOf(new LinkedHashSet<>(mapper.findVisibleMenusByUserId(user.id())));
        return new UserAuthoritySnapshot(user, roles, permissions, menus);
    }

    private List<String> distinctSorted(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
```

Create `AuditLogService`:

```java
package com.example.aiticket.system;

import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final UserSecurityMapper mapper;

    public AuditLogService(UserSecurityMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long actorUserId, String actorUsername, String action, String targetType,
                       String targetId, String result, String message, String ipAddress, String userAgent) {
        mapper.insertAuditLog(actorUserId, actorUsername, action, targetType, targetId, result, message, ipAddress, userAgent);
    }
}
```

- [x] **Step 3: Add mapper XML** ⭐

Create `backend/src/main/resources/mapper/UserSecurityMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.aiticket.system.UserSecurityMapper">
    <select id="findActiveUserByUsername" resultType="com.example.aiticket.system.UserAccount">
        SELECT id, username, password_hash AS passwordHash, display_name AS displayName, status, token_version AS tokenVersion
        FROM sys_user
        WHERE username = #{username}
          AND status = 'ACTIVE'
    </select>

    <select id="findActiveUserById" resultType="com.example.aiticket.system.UserAccount">
        SELECT id, username, password_hash AS passwordHash, display_name AS displayName, status, token_version AS tokenVersion
        FROM sys_user
        WHERE id = #{userId}
          AND status = 'ACTIVE'
    </select>

    <select id="findRoleCodesByUserId" resultType="string">
        SELECT r.role_code
        FROM sys_role r
        JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.status = 'ACTIVE'
        ORDER BY r.sort_order, r.role_code
    </select>

    <select id="findPermissionCodesByUserId" resultType="string">
        SELECT DISTINCT p.permission_code
        FROM sys_permission p
        JOIN sys_role_permission rp ON rp.permission_id = p.id
        JOIN sys_user_role ur ON ur.role_id = rp.role_id
        JOIN sys_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND p.status = 'ACTIVE'
          AND r.status = 'ACTIVE'
        ORDER BY p.permission_code
    </select>

    <select id="findVisibleMenusByUserId" resultType="com.example.aiticket.system.MenuSummary">
        SELECT m.menu_code AS code, m.menu_name AS name, m.route_path AS path, m.icon
        FROM sys_menu m
        WHERE m.visible = 1
          AND m.status = 'ACTIVE'
          AND (
              m.permission_code IS NULL
              OR m.permission_code IN (
                  SELECT DISTINCT p.permission_code
                  FROM sys_permission p
                  JOIN sys_role_permission rp ON rp.permission_id = p.id
                  JOIN sys_user_role ur ON ur.role_id = rp.role_id
                  JOIN sys_role r ON r.id = ur.role_id
                  WHERE ur.user_id = #{userId}
                    AND p.status = 'ACTIVE'
                    AND r.status = 'ACTIVE'
              )
          )
        ORDER BY m.sort_order, m.menu_code
    </select>

    <update id="updateLastLoginAt">
        UPDATE sys_user
        SET last_login_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{userId}
    </update>

    <insert id="insertAuditLog">
        INSERT INTO audit_log (
            actor_user_id, actor_username, action, target_type, target_id,
            result, message, ip_address, user_agent
        )
        VALUES (
            #{actorUserId}, #{actorUsername}, #{action}, #{targetType}, #{targetId},
            #{result}, #{message}, #{ipAddress}, #{userAgent}
        )
    </insert>
</mapper>
```

- [x] **Step 4: Run focused service test** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=UserAuthorityServiceTest test
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 5: Commit** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/java/com/example/aiticket/system backend/src/main/resources/mapper/UserSecurityMapper.xml backend/src/test/java/com/example/aiticket/system/UserAuthorityServiceTest.java docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add user authority query layer"
```

---

## Task 4: Add JWT Service And Security Principal

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/security/JwtService.java`
- Create: `backend/src/main/java/com/example/aiticket/security/JwtClaims.java`
- Create: `backend/src/main/java/com/example/aiticket/security/AuthenticatedUser.java`
- Create: `backend/src/test/java/com/example/aiticket/security/JwtServiceTest.java`

- [ ] **Step 1: Write JWT service tests**

Create `JwtServiceTest` to assert token generation, parsing, and expiry:

```java
package com.example.aiticket.security;

import com.example.aiticket.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    @Test
    void createsAndParsesToken() {
        JwtService service = new JwtService(properties(7200));

        String token = service.createAccessToken(1L, "admin", 0);
        JwtClaims claims = service.parse(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.tokenVersion()).isEqualTo(0);
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService service = new JwtService(properties(1));
        String token = service.createAccessToken(1L, "admin", 0);

        Thread.sleep(1200);

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid jwt token");
    }

    private JwtProperties properties(long expiresInSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("local-dev-secret-with-at-least-32-characters");
        properties.setExpiresInSeconds(expiresInSeconds);
        return properties;
    }
}
```

- [ ] **Step 2: Implement JWT service**

Create `JwtClaims`:

```java
package com.example.aiticket.security;

public record JwtClaims(Long userId, String username, int tokenVersion) {
}
```

Create `JwtService`:

```java
package com.example.aiticket.security;

import com.example.aiticket.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpiresInSeconds());
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public JwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number userId = claims.get("userId", Number.class);
            Number tokenVersion = claims.get("tokenVersion", Number.class);
            return new JwtClaims(userId.longValue(), claims.getSubject(), tokenVersion.intValue());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("invalid jwt token", ex);
        }
    }

    public long expiresInSeconds() {
        return properties.getExpiresInSeconds();
    }
}
```

Create `AuthenticatedUser`:

```java
package com.example.aiticket.security;

import java.util.List;

public record AuthenticatedUser(
        Long id,
        String username,
        String displayName,
        int tokenVersion,
        List<String> roles,
        List<String> permissions
) {
}
```

- [ ] **Step 3: Run focused JWT tests**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=JwtServiceTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 4: Commit**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/java/com/example/aiticket/security backend/src/test/java/com/example/aiticket/security/JwtServiceTest.java docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add jwt service"
```

---

## Task 5: Add Auth Service And Controllers

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/example/aiticket/auth/LoginResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/auth/CurrentUserResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/auth/AuthService.java`
- Create: `backend/src/main/java/com/example/aiticket/auth/AuthController.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/AdminPingController.java`
- Create: `backend/src/test/java/com/example/aiticket/auth/AuthServiceTest.java`

- [ ] **Step 1: Write AuthService tests**

Create `AuthServiceTest` covering successful login and unified failure message:

```java
package com.example.aiticket.auth;

import com.example.aiticket.security.JwtService;
import com.example.aiticket.system.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void loginReturnsTokenAndAuthoritySummary() {
        UserAuthorityService authorityService = mock(UserAuthorityService.class);
        UserSecurityMapper mapper = mock(UserSecurityMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(authorityService, mapper, auditLogService, jwtService, passwordEncoder);
        UserAccount user = new UserAccount(1L, "admin", passwordEncoder.encode("Admin_123456"), "系统管理员", "ACTIVE", 0);
        UserAuthoritySnapshot snapshot = new UserAuthoritySnapshot(
                user,
                List.of("ADMIN"),
                List.of("system:user:manage"),
                List.of(new MenuSummary("users", "用户权限管理", "/admin/users", "User"))
        );
        when(authorityService.loadByUsername("admin")).thenReturn(Optional.of(snapshot));
        when(jwtService.createAccessToken(1L, "admin", 0)).thenReturn("token");
        when(jwtService.expiresInSeconds()).thenReturn(7200L);

        LoginResponse response = service.login(new LoginRequest("admin", "Admin_123456"), "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.permissions()).containsExactly("system:user:manage");
        verify(mapper).updateLastLoginAt(1L);
        verify(auditLogService).record(1L, "admin", "AUTH_LOGIN_SUCCESS", "AUTH", "1", "SUCCESS", "login success", "127.0.0.1", "JUnit");
    }

    @Test
    void loginFailureDoesNotExposeWhetherUserExists() {
        UserAuthorityService authorityService = mock(UserAuthorityService.class);
        AuthService service = new AuthService(
                authorityService,
                mock(UserSecurityMapper.class),
                mock(AuditLogService.class),
                mock(JwtService.class),
                new BCryptPasswordEncoder()
        );
        when(authorityService.loadByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("missing", "bad"), "127.0.0.1", "JUnit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名或密码错误");
    }
}
```

- [ ] **Step 2: Implement DTOs and service**

Create DTOs and `AuthService` so the tests pass. `CurrentUserResponse.UserSummary` must include `id`, `username`, and `displayName`.

- [ ] **Step 3: Implement controllers**

`AuthController`:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.currentUser(principal.id()));
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }
}
```

`AdminPingController`:

```java
@RestController
@RequestMapping("/api/admin")
public class AdminPingController {
    @PreAuthorize("hasAuthority('system:user:manage')")
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("admin-pong");
    }
}
```

- [ ] **Step 4: Run focused AuthService tests**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=AuthServiceTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/java/com/example/aiticket/auth backend/src/main/java/com/example/aiticket/admin backend/src/test/java/com/example/aiticket/auth/AuthServiceTest.java docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add auth login service"
```

---

## Task 6: Add Spring Security Filter Chain

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/example/aiticket/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/example/aiticket/common/api/ApiResponse.java`

- [ ] **Step 1: Implement SecurityConfig**

Security rules:

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers("/api/spike/**").permitAll()
            .anyRequest().authenticated()
    )
    .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized"))
            .accessDeniedHandler((request, response, accessDeniedException) -> writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden"))
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

Enable method security:

```java
@EnableMethodSecurity
```

Expose:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- [ ] **Step 2: Implement JwtAuthenticationFilter**

Filter behavior:

1. If header missing or not `Bearer `, continue without auth.
2. Parse token with `JwtService`.
3. Load snapshot by `userId`.
4. Reject if token version differs.
5. Build `UsernamePasswordAuthenticationToken` using `AuthenticatedUser` and `SimpleGrantedAuthority`.

- [ ] **Step 3: Add ApiResponse failure factory**

Modify `ApiResponse`:

```java
public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(false, null, message);
}
```

- [ ] **Step 4: Run full tests**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/java/com/example/aiticket/security backend/src/main/java/com/example/aiticket/common/api/ApiResponse.java docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "feat: add jwt security filter chain"
```

---

## Task 7: Run Live Auth/RBAC Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md`

- [ ] **Step 1: Start Docker services if needed**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
docker compose ps
```

Expected:

```text
ai-ticket-oracle ... healthy
ai-ticket-redis ... healthy
```

- [ ] **Step 2: Start backend**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

Expected:

```text
Started AiTicketApplication
```

- [ ] **Step 3: Verify login succeeds**

Run:

```bash
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin_123456"}'
```

Expected:

```text
"success":true
"tokenType":"Bearer"
"accessToken"
```

Do not commit or paste a real long-lived token into docs. Use temporary shell variables for follow-up calls.

- [ ] **Step 4: Verify protected endpoints**

Use the token from login:

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin_123456"}' | node -e 'let s=""; process.stdin.on("data", d => s += d); process.stdin.on("end", () => console.log(JSON.parse(s).data.accessToken));')
curl -sS http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
curl -sS http://localhost:8080/api/auth/ping -H "Authorization: Bearer $TOKEN"
curl -sS http://localhost:8080/api/admin/ping -H "Authorization: Bearer $TOKEN"
```

Expected:

```text
当前用户摘要 includes admin roles/permissions/menus
pong
admin-pong
```

- [ ] **Step 5: Verify unauthorized and forbidden**

Run unauthenticated ping:

```bash
curl -i -sS http://localhost:8080/api/auth/ping
```

Expected:

```text
HTTP/1.1 401
```

Login as `user`, call admin ping:

```bash
USER_TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"user","password":"Admin_123456"}' | node -e 'let s=""; process.stdin.on("data", d => s += d); process.stdin.on("end", () => console.log(JSON.parse(s).data.accessToken));')
curl -i -sS http://localhost:8080/api/admin/ping -H "Authorization: Bearer $USER_TOKEN"
```

Expected:

```text
HTTP/1.1 403
```

- [ ] **Step 6: Commit final plan progress**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add docs/superpowers/plans/2026-06-19-auth-rbac-backend-implementation-plan.md
git commit -m "docs: record auth rbac verification"
```

---

## Self-Review

Spec coverage:

- RBAC tables and audit log are covered by Task 2.
- JWT config, token version, and stable token claims are covered by Tasks 1 and 4.
- User/role/permission/menu authority loading is covered by Task 3.
- Login, current user, auth ping, and admin ping are covered by Task 5.
- Security filter chain, 401, 403, and method security are covered by Task 6 and Task 7.
- Extensibility is preserved through `department_id`, `data_scope`, `token_version`, `sys_menu`, `audit_log`, and service boundaries.

Placeholder scan:

- This plan intentionally avoids “implement later” tasks inside the current scope.
- Out-of-scope CRUD, Redis permission cache, department model, data-scope filtering, and frontend work are explicitly excluded.

Type consistency:

- `JwtProperties` lives in `com.example.aiticket.config`.
- `JwtService`, `JwtClaims`, `AuthenticatedUser`, `SecurityConfig`, and `JwtAuthenticationFilter` live in `com.example.aiticket.security`.
- RBAC persistence classes live in `com.example.aiticket.system`.
- Auth endpoint classes live in `com.example.aiticket.auth`.
