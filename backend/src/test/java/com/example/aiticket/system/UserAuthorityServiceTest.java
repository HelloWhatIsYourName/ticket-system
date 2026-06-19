package com.example.aiticket.system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthorityServiceTest {
    @Test
    void loadsSnapshotWithDistinctRolesPermissionsAndMenus() {
        UserSecurityMapper mapper = new FakeUserSecurityMapper();
        UserAuthorityService service = new UserAuthorityService(mapper);

        UserAuthoritySnapshot snapshot = service.loadByUsername("admin").orElseThrow();

        assertThat(snapshot.user()).isEqualTo(FakeUserSecurityMapper.ACCOUNT);
        assertThat(snapshot.roles()).containsExactly("ADMIN");
        assertThat(snapshot.permissions()).containsExactly("dashboard:view", "system:user:manage");
        assertThat(snapshot.menus()).extracting(MenuSummary::code).containsExactly("users", "dashboard");
    }

    private static class FakeUserSecurityMapper implements UserSecurityMapper {
        static final UserAccount ACCOUNT = new UserAccount(1L, "admin", "$hash", "系统管理员", "ACTIVE", 0);

        @Override
        public Optional<UserAccount> findActiveUserByUsername(String username) {
            return Optional.of(ACCOUNT);
        }

        @Override
        public Optional<UserAccount> findActiveUserById(Long userId) {
            return Optional.of(ACCOUNT);
        }

        @Override
        public List<String> findRoleCodesByUserId(Long userId) {
            return List.of("ADMIN", "ADMIN");
        }

        @Override
        public List<String> findPermissionCodesByUserId(Long userId) {
            return List.of("system:user:manage", "dashboard:view", "dashboard:view");
        }

        @Override
        public List<MenuSummary> findVisibleMenusByUserId(Long userId) {
            return List.of(
                    new MenuSummary("users", "用户权限管理", "/admin/users", "User"),
                    new MenuSummary("dashboard", "统计看板", "/admin/dashboard", "Chart")
            );
        }

        @Override
        public void updateLastLoginAt(Long userId) {
        }

        @Override
        public void insertAuditLog(Long actorUserId, String actorUsername, String action, String targetType,
                                   String targetId, String result, String message, String ipAddress, String userAgent) {
        }
    }
}
