package com.example.aiticket.docs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MenuRouteAlignmentTest {
    private static final Map<String, String> EXPECTED_ROUTES = Map.of(
            "chat", "/app/ai/chat",
            "tickets", "/app/tickets/my",
            "agent-workbench", "/app/tickets/assigned",
            "knowledge", "/app/knowledge",
            "users", "/app/system",
            "dashboard", "/app/admin/dashboard"
    );

    @Test
    void authSeedUsesImplementedFrontendAppRoutes() throws Exception {
        String migration = Files.readString(Path.of("../backend/src/main/resources/db/migration/V2__auth_rbac.sql"));

        EXPECTED_ROUTES.forEach((menuCode, routePath) -> {
            assertThat(migration).contains("'%s'".formatted(menuCode));
            assertThat(migration).contains("'%s'".formatted(routePath));
        });

        assertThat(migration).doesNotContain("'/chat'");
        assertThat(migration).doesNotContain("'/tickets'");
        assertThat(migration).doesNotContain("'/agent/tickets'");
        assertThat(migration).doesNotContain("'/admin/knowledge'");
        assertThat(migration).doesNotContain("'/admin/users'");
        assertThat(migration).doesNotContain("'/admin/dashboard'");
    }

    @Test
    void routeCorrectionMigrationUpdatesExistingEnvironments() throws Exception {
        Path correctionPath = Path.of("../backend/src/main/resources/db/migration/V6__frontend_route_alignment.sql");
        assertThat(correctionPath).exists();

        String migration = Files.readString(correctionPath);
        EXPECTED_ROUTES.forEach((menuCode, routePath) -> {
            assertThat(migration).contains("WHERE menu_code = '%s'".formatted(menuCode));
            assertThat(migration).contains("route_path = '%s'".formatted(routePath));
        });
    }
}
