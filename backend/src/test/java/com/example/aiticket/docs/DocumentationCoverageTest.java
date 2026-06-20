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
        assertThat(script).contains("/api/kb/documents/text");
        assertThat(script).contains("/api/kb/search");
        assertThat(script).doesNotContain("/api/knowledge/documents/text");
        assertThat(script).doesNotContain("/api/knowledge/search");
        assertThat(script).contains("/api/ai/chat/ask");
        assertThat(script).contains("/api/tickets/from-ai-session");
        assertThat(script).contains("/api/admin/statistics/overview");
        assertThat(script).contains("/api/admin/users");
        assertThat(script).contains("token:redacted");
        assertThat(script).doesNotContain("echo \"$ADMIN_TOKEN\"");
        assertThat(script).doesNotContain("echo \"$USER_TOKEN\"");
    }

    @Test
    void acceptanceAndDemoDocsCoverMajorFirstVersionModules() throws Exception {
        assertDocumentContainsMajorModules(Path.of("../docs/acceptance/v1-acceptance-checklist.md"));
        assertDocumentContainsMajorModules(Path.of("../docs/demo/v1-demo-runbook.md"));
    }

    @Test
    void liveRehearsalMaterialsCoverFinalDefenseEvidenceWithoutPrintingSecrets() throws Exception {
        Path checklistPath = Path.of("../docs/demo/v1-live-rehearsal-checklist.md");
        Path reportPath = Path.of("../docs/evaluation/rag-live-evaluation-report.md");
        Path scriptPath = Path.of("../tools/smoke/phase19-demo-preflight.sh");
        assertThat(checklistPath).exists();
        assertThat(reportPath).exists();
        assertThat(scriptPath).exists();

        String checklist = Files.readString(checklistPath);
        assertThat(checklist).contains("/app/demo");
        assertThat(checklist).contains("/app/knowledge");
        assertThat(checklist).contains("/app/ai/chat");
        assertThat(checklist).contains("/app/tickets/my");
        assertThat(checklist).contains("/app/tickets/assigned");
        assertThat(checklist).contains("/app/admin/dashboard");
        assertThat(checklist).contains("/app/system");
        assertThat(checklist).contains("Oracle 23ai");
        assertThat(checklist).contains("Redis");
        assertThat(checklist).contains("SiliconFlow");
        assertThat(checklist).contains("DeepSeek");
        assertThat(checklist).contains("token:redacted");

        String report = Files.readString(reportPath);
        assertThat(report).contains("docs/evaluation/rag-evaluation-set.json");
        assertThat(report).contains("RAG-001");
        assertThat(report).contains("RAG-020");
        assertThat(report).contains("检索命中率");
        assertThat(report).contains("回答有用率");
        assertThat(report).contains("误转工单率");
        assertThat(report).contains("应转未转率");

        String script = Files.readString(scriptPath);
        assertThat(script).contains("/api/auth/login");
        assertThat(script).contains("/api/auth/me");
        assertThat(script).contains("/api/kb/search");
        assertThat(script).contains("/api/ai/chat/ask");
        assertThat(script).contains("/api/ai/chat/stream");
        assertThat(script).contains("/api/admin/statistics/overview");
        assertThat(script).contains("token:redacted");
        assertThat(script).doesNotContain("echo \"$ADMIN_TOKEN\"");
        assertThat(script).doesNotContain("echo \"$USER_TOKEN\"");
    }

    @Test
    void originalV1PlanReflectsCurrentImplementedScope() throws Exception {
        Path projectPlanPath = Path.of("../docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md");
        assertThat(projectPlanPath).exists();

        String projectPlan = Files.readString(projectPlanPath);
        assertThat(projectPlan).contains("Phase 19");
        assertThat(projectPlan).contains("前端 RAG chat");
        assertThat(projectPlan).contains("工单列表/详情");
        assertThat(projectPlan).contains("知识库管理");
        assertThat(projectPlan).contains("系统管理");
        assertThat(projectPlan).contains("文件上传");
        assertThat(projectPlan).contains("SSE");
        assertThat(projectPlan).contains("live-provider rehearsal");
        assertThat(projectPlan).doesNotContain("下一步推进剩余前端业务页");
    }

    private void assertDocumentContainsMajorModules(Path path) throws Exception {
        assertThat(path).exists();
        String document = Files.readString(path);

        assertThat(document).contains("RBAC");
        assertThat(document).contains("知识库");
        assertThat(document).contains("RAG");
        assertThat(document).contains("工单");
        assertThat(document).contains("统计");
        assertThat(document).contains("Redis");
    }
}
