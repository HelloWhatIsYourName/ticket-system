package com.example.aiticket.docs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationCoverageTest {
    @Test
    void phase7SmokeScriptCoversCoreBackendEndpointsWithoutPrintingTokens() throws Exception {
        Path scriptPath = Path.of("../tools/smoke/phase7-backend-smoke.sh");
        assertThat(scriptPath).exists();
        String script = Files.readString(scriptPath);

        assertThat(script).contains("/api/auth/login");
        assertThat(script).contains("/api/auth/me");
        assertThat(script).contains("/api/knowledge/documents/text");
        assertThat(script).contains("/api/knowledge/search");
        assertThat(script).contains("/api/ai/chat/ask");
        assertThat(script).contains("/api/tickets/from-ai-session");
        assertThat(script).contains("/api/admin/statistics/overview");
        assertThat(script).contains("/api/admin/users");
        assertThat(script).contains("token:redacted");
        assertThat(script).doesNotContain("echo \"$ADMIN_TOKEN\"");
        assertThat(script).doesNotContain("echo \"$USER_TOKEN\"");
    }
}
