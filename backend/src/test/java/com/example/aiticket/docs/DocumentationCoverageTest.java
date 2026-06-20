package com.example.aiticket.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationCoverageTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void phase7SmokeScriptCoversCoreBackendEndpointsWithoutPrintingTokens() throws Exception {
        Path scriptPath = Path.of("../tools/smoke/phase7-backend-smoke.sh");
        assertThat(scriptPath).exists();
        String script = Files.readString(scriptPath);

        assertThat(script).contains("/api/auth/login");
        assertThat(script).contains("/api/auth/me");
        assertThat(script).contains("/api/kb/documents/text");
        assertThat(script).contains("/api/kb/search");
        assertThat(script).contains("check knowledgeSearch 200 POST /api/kb/search \"$ADMIN_TOKEN\"");
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
        assertThat(script).contains("check knowledgeSearch 200 POST /api/kb/search \"$ADMIN_TOKEN\"");
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

    @Test
    void liveRehearsalAuditRecordsBlockedPrerequisitesWithoutPrintingSecrets() throws Exception {
        Path reportPath = Path.of("../docs/demo/v1-live-rehearsal-audit.md");
        Path scriptPath = Path.of("../tools/smoke/phase21-rehearsal-audit.sh");
        assertThat(reportPath).exists();
        assertThat(scriptPath).exists();

        String report = Files.readString(reportPath);
        assertThat(report).contains("Docker services");
        assertThat(report).contains("Oracle 23ai");
        assertThat(report).contains("Redis");
        assertThat(report).contains("Backend");
        assertThat(report).contains("Frontend");
        assertThat(report).contains("AI_EMBEDDING_API_KEY");
        assertThat(report).contains("AI_CHAT_API_KEY");
        assertThat(report).contains("PASS");
        assertThat(report).contains("Phase 19 preflight");
        assertThat(report).contains("token:redacted");

        String script = Files.readString(scriptPath);
        assertThat(script).contains("docker compose ps");
        assertThat(script).contains("/api/auth/me");
        assertThat(script).contains("127.0.0.1:5174");
        assertThat(script).contains("phase19-demo-preflight.sh");
        assertThat(script).contains("token:redacted");
        assertThat(script).doesNotContain("echo \"$AI_CHAT_API_KEY\"");
        assertThat(script).doesNotContain("echo \"$AI_EMBEDDING_API_KEY\"");
    }

    @Test
    void phase23DemoCorpusAndRagEvaluationScriptsAreRepeatableAndSecretSafe() throws Exception {
        Path loadScriptPath = Path.of("../tools/smoke/phase23-load-demo-corpus.sh");
        Path evaluationScriptPath = Path.of("../tools/smoke/phase23-run-rag-evaluation.sh");
        Path scorerPath = Path.of("../tools/smoke/rag-evaluation-scorer.cjs");
        Path scorerTestPath = Path.of("../tools/smoke/rag-evaluation-scorer.test.cjs");
        Path reportPath = Path.of("../docs/evaluation/rag-live-evaluation-report.md");
        Path datasetPath = Path.of("../docs/evaluation/rag-evaluation-set.json");

        assertThat(loadScriptPath).exists();
        assertThat(evaluationScriptPath).exists();
        assertThat(scorerPath).exists();
        assertThat(scorerTestPath).exists();
        assertThat(reportPath).exists();
        assertThat(datasetPath).exists();

        String loadScript = Files.readString(loadScriptPath);
        assertThat(loadScript).contains("docs/demo/v1-demo-corpus.json");
        assertThat(loadScript).contains("/api/auth/login");
        assertThat(loadScript).contains("/api/kb/documents/text");
        assertThat(loadScript).contains("categoryId");
        assertThat(loadScript).contains("token:redacted");
        assertThat(loadScript).doesNotContain("echo \"$ADMIN_TOKEN\"");
        assertThat(loadScript).doesNotContain("echo \"$AI_CHAT_API_KEY\"");
        assertThat(loadScript).doesNotContain("echo \"$AI_EMBEDDING_API_KEY\"");

        String evaluationScript = Files.readString(evaluationScriptPath);
        assertThat(evaluationScript).contains("docs/evaluation/rag-evaluation-set.json");
        assertThat(evaluationScript).contains("/api/auth/login");
        assertThat(evaluationScript).contains("/api/ai/chat/ask");
        assertThat(evaluationScript).contains("RESULTS_PATH");
        assertThat(evaluationScript).contains("retrievalHitRate");
        assertThat(evaluationScript).contains("answerUsefulRate");
        assertThat(evaluationScript).contains("wrongTransferRate");
        assertThat(evaluationScript).contains("missedTransferRate");
        assertThat(evaluationScript).contains("SCORER_PATH");
        assertThat(evaluationScript).contains("node --input-type=commonjs <<'NODE'");
        assertThat(evaluationScript).contains("main().catch");
        assertThat(evaluationScript).contains("token:redacted");
        assertThat(evaluationScript).doesNotContain("echo \"$USER_TOKEN\"");
        assertThat(evaluationScript).doesNotContain("echo \"$AI_CHAT_API_KEY\"");
        assertThat(evaluationScript).doesNotContain("echo \"$AI_EMBEDDING_API_KEY\"");

        String scorer = Files.readString(scorerPath);
        assertThat(scorer).contains("sourceHintAliases");
        assertThat(scorer).contains("keywordAliases");
        assertThat(scorer).contains("retrievalHit");
        assertThat(scorer).contains("summarizeResults");

        String report = Files.readString(reportPath);
        assertThat(report).contains("tools/smoke/phase23-load-demo-corpus.sh");
        assertThat(report).contains("tools/smoke/phase23-run-rag-evaluation.sh");
        JsonNode cases = objectMapper.readTree(Files.readString(datasetPath));
        for (JsonNode evaluationCase : cases) {
            assertThat(report).contains(evaluationCase.path("id").asText());
            assertThat(report).contains(evaluationCase.path("question").asText());
        }
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
