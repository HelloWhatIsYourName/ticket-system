package com.example.aiticket.auth;

import com.example.aiticket.config.JwtProperties;
import com.example.aiticket.security.JwtService;
import com.example.aiticket.system.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    @Test
    void loginReturnsTokenAndAuthoritySummary() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        FakeUserSecurityMapper mapper = new FakeUserSecurityMapper(passwordEncoder.encode("Admin_123456"));
        AuthService service = new AuthService(
                new UserAuthorityService(mapper),
                mapper,
                new AuditLogService(mapper),
                new JwtService(jwtProperties()),
                passwordEncoder
        );

        LoginResponse response = service.login(new LoginRequest("admin", "Admin_123456"), "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.permissions()).containsExactly("system:user:manage");
        assertThat(response.menus()).extracting(MenuSummary::code).containsExactly("users");
        assertThat(mapper.lastLoginUpdatedUserId).isEqualTo(1L);
        assertThat(mapper.auditAction).isEqualTo("AUTH_LOGIN_SUCCESS");
        assertThat(mapper.auditResult).isEqualTo("SUCCESS");
    }

    @Test
    void loginFailureDoesNotExposeWhetherUserExists() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        FakeUserSecurityMapper mapper = new FakeUserSecurityMapper(passwordEncoder.encode("Admin_123456"));
        AuthService service = new AuthService(
                new UserAuthorityService(mapper),
                mapper,
                new AuditLogService(mapper),
                new JwtService(jwtProperties()),
                passwordEncoder
        );

        assertThatThrownBy(() -> service.login(new LoginRequest("missing", "bad"), "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidLoginException.class)
                .hasMessage("用户名或密码错误");
        assertThat(mapper.auditAction).isEqualTo("AUTH_LOGIN_FAILURE");
        assertThat(mapper.auditResult).isEqualTo("FAILURE");
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("local-dev-secret-with-at-least-32-characters");
        properties.setExpiresInSeconds(7200);
        return properties;
    }

    private static class FakeUserSecurityMapper implements UserSecurityMapper {
        private final String passwordHash;
        private Long lastLoginUpdatedUserId;
        private String auditAction;
        private String auditResult;

        private FakeUserSecurityMapper(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        @Override
        public Optional<UserAccount> findActiveUserByUsername(String username) {
            if (!"admin".equals(username)) {
                return Optional.empty();
            }
            return Optional.of(new UserAccount(1L, "admin", passwordHash, "系统管理员", "ACTIVE", 0));
        }

        @Override
        public Optional<UserAccount> findActiveUserById(Long userId) {
            return Optional.of(new UserAccount(1L, "admin", passwordHash, "系统管理员", "ACTIVE", 0));
        }

        @Override
        public List<String> findRoleCodesByUserId(Long userId) {
            return List.of("ADMIN");
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long userId) {
            return List.of("system:user:manage");
        }

        @Override
        public List<MenuSummary> findVisibleMenusByUserId(Long userId) {
            return List.of(new MenuSummary("users", "用户权限管理", "/admin/users", "Users"));
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            this.lastLoginUpdatedUserId = userId;
        }

        @Override
        public void insertAuditLog(Long actorUserId, String actorUsername, String action, String targetType,
                                   String targetId, String result, String message, String ipAddress, String userAgent) {
            this.auditAction = action;
            this.auditResult = result;
        }
    }
}
