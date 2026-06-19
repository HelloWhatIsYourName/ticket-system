# SiliconFlow Embedding Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. When a step is completed, append `⭐` to that checked line, for example `- [x] **Step 1: ...** ⭐`.

**Goal:** Replace the first-version Embedding provider from Aliyun Bailian to SiliconFlow while preserving the verified Oracle `VECTOR(1024, FLOAT32)` storage and retrieval path.

**Architecture:** Keep the existing `EmbeddingClient` interface and `EmbeddingResult` contract stable so future knowledge-base code depends only on the abstraction. Replace the concrete provider adapter with a SiliconFlow OpenAI-compatible `/embeddings` client, keep configuration provider-specific through `ai.embedding.*`, and validate the switch through unit tests plus optional live provider verification when `AI_EMBEDDING_API_KEY` is available.

**Tech Stack:** Java 21, Spring Boot 3, Spring `RestClient`, Maven, JUnit 5, AssertJ, Oracle Database 23ai Free, MyBatis, Flyway, Redis, SiliconFlow Embeddings API, `Qwen/Qwen3-Embedding-8B`.

---

## File Structure

Files to modify:

- `backend/src/main/resources/application.yml`
  - Change default Embedding provider, base URL, model, and keep dimensions at `1024`.
- `backend/src/test/java/com/example/aiticket/config/AiProviderPropertiesTest.java`
  - Update configuration binding expectations for SiliconFlow defaults.
- `backend/src/test/java/com/example/aiticket/ai/embedding/EmbeddingResultTest.java`
  - Update model name used in dimension validation test.
- `backend/src/main/java/com/example/aiticket/ai/embedding/AliyunBailianEmbeddingClient.java`
  - Replace with `SiliconFlowEmbeddingClient` by deleting the old provider-specific class and creating the new provider-specific class.
- `backend/src/test/java/com/example/aiticket/ai/embedding/SiliconFlowEmbeddingClientTest.java`
  - Add focused client tests for validation, request payload, response ordering, and dimension enforcement.
- `README.md`
  - Replace first-version Embedding provider description.
- `docs/spikes/oracle-vector-spike.md`
  - Replace Aliyun live verification checklist with SiliconFlow, and later record live verification only if it actually runs.
- `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
  - Replace first-version Embedding provider references.
- `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-system-design.md`
  - Replace architecture, stack, schema, workflow, and config references.
- `docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md`
  - Add a correction note that the first-version provider changed to SiliconFlow after the spike.
- `沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
  - Mirror project-plan provider references for communication material.
- `沟通材料/2026-06-19-ai-knowledge-ticket-system-design.md`
  - Mirror system-design provider references for communication material.

Files not to modify:

- `backend/src/main/java/com/example/aiticket/ai/embedding/EmbeddingClient.java`
  - The abstraction remains stable.
- `backend/src/main/java/com/example/aiticket/ai/embedding/EmbeddingResult.java`
  - The dimension guard remains provider-neutral.
- `backend/src/main/resources/db/migration/V1__vector_spike.sql`
  - Oracle vector shape remains `VECTOR(1024, FLOAT32)`.
- `backend/src/main/resources/mapper/VectorSpikeMapper.xml`
  - Existing `TO_VECTOR(...)` and `VECTOR_DISTANCE(..., COSINE)` SQL remains verified and unchanged.

---

## Task 1: Update Configuration Binding Expectations

**Files:**
- Modify: `backend/src/test/java/com/example/aiticket/config/AiProviderPropertiesTest.java`
- Modify: `backend/src/main/resources/application.yml`

- [x] **Step 1: Write the failing config binding expectation** ⭐

Replace the embedding block and assertions in `backend/src/test/java/com/example/aiticket/config/AiProviderPropertiesTest.java` with SiliconFlow values:

```java
                  embedding:
                    provider: siliconflow
                    base-url: https://api.siliconflow.cn/v1
                    api-key: embedding-key
                    model: Qwen/Qwen3-Embedding-8B
                    dimensions: 1024
```

```java
        assertThat(properties.getEmbedding().getProvider()).isEqualTo("siliconflow");
        assertThat(properties.getEmbedding().getBaseUrl()).isEqualTo("https://api.siliconflow.cn/v1");
        assertThat(properties.getEmbedding().getModel()).isEqualTo("Qwen/Qwen3-Embedding-8B");
        assertThat(properties.getEmbedding().getDimensions()).isEqualTo(1024);
```

- [x] **Step 2: Run the focused test** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=AiProviderPropertiesTest test
```

Expected:

```text
BUILD SUCCESS
```

This test binds an inline YAML fixture, so it should pass after Step 1 even before `application.yml` is changed.

- [x] **Step 3: Update application defaults** ⭐

Change `backend/src/main/resources/application.yml`:

```yaml
  embedding:
    provider: siliconflow
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.siliconflow.cn/v1}
    api-key: ${AI_EMBEDDING_API_KEY:}
    model: ${AI_EMBEDDING_MODEL:Qwen/Qwen3-Embedding-8B}
    dimensions: ${AI_EMBEDDING_DIMENSIONS:1024}
```

- [x] **Step 4: Run the focused test again** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=AiProviderPropertiesTest test
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 5: Commit configuration switch** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/test/java/com/example/aiticket/config/AiProviderPropertiesTest.java backend/src/main/resources/application.yml
git commit -m "feat: default embedding provider to siliconflow"
```

---

## Task 2: Replace Provider Adapter With SiliconFlow Client

**Files:**
- Delete: `backend/src/main/java/com/example/aiticket/ai/embedding/AliyunBailianEmbeddingClient.java`
- Create: `backend/src/main/java/com/example/aiticket/ai/embedding/SiliconFlowEmbeddingClient.java`
- Create: `backend/src/test/java/com/example/aiticket/ai/embedding/SiliconFlowEmbeddingClientTest.java`
- Modify: `backend/src/test/java/com/example/aiticket/ai/embedding/EmbeddingResultTest.java`

- [x] **Step 1: Add the failing SiliconFlow client test** ⭐

Create `backend/src/test/java/com/example/aiticket/ai/embedding/SiliconFlowEmbeddingClientTest.java`:

```java
package com.example.aiticket.ai.embedding;

import com.example.aiticket.config.AiProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;

class SiliconFlowEmbeddingClientTest {
    @Test
    void sendsSiliconFlowEmbeddingRequestAndSortsResultsByIndex() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        server.expect(once(), requestTo("https://api.siliconflow.cn/v1/embeddings"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("Qwen/Qwen3-Embedding-8B"))
                .andExpect(jsonPath("$.input[0]").value("first text"))
                .andExpect(jsonPath("$.input[1]").value("second text"))
                .andExpect(jsonPath("$.dimensions").value(3))
                .andExpect(jsonPath("$.encoding_format").value("float"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 1, "embedding": [0.4, 0.5, 0.6]},
                            {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<EmbeddingResult> results = client.embedBatch(List.of("first text", "second text"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).model()).isEqualTo("Qwen/Qwen3-Embedding-8B");
        assertThat(results.get(0).dimensions()).isEqualTo(3);
        assertThat(results.get(0).vector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(results.get(1).vector()).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    @Test
    void rejectsBlankTextsBeforeCallingProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        assertThatThrownBy(() -> client.embedBatch(List.of("valid", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("texts must not contain blank values");

        server.verify();
    }

    @Test
    void rejectsMismatchedResponseSize() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        server.expect(once(), requestTo("https://api.siliconflow.cn/v1/embeddings"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embedBatch(List.of("first", "second")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("embedding response size does not match request size");

        server.verify();
    }

    private AiProviderProperties properties() {
        AiProviderProperties properties = new AiProviderProperties();
        AiProviderProperties.EmbeddingProvider embedding = new AiProviderProperties.EmbeddingProvider();
        embedding.setProvider("siliconflow");
        embedding.setBaseUrl("https://api.siliconflow.cn/v1");
        embedding.setApiKey("test-key");
        embedding.setModel("Qwen/Qwen3-Embedding-8B");
        embedding.setDimensions(3);
        properties.setEmbedding(embedding);
        return properties;
    }
}
```

- [x] **Step 2: Run the focused client test to verify it fails** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=SiliconFlowEmbeddingClientTest test
```

Expected:

```text
Compilation failure
cannot find symbol
  symbol:   class SiliconFlowEmbeddingClient
```

- [x] **Step 3: Create `SiliconFlowEmbeddingClient`** ⭐

Delete `backend/src/main/java/com/example/aiticket/ai/embedding/AliyunBailianEmbeddingClient.java`.

Create `backend/src/main/java/com/example/aiticket/ai/embedding/SiliconFlowEmbeddingClient.java`:

```java
package com.example.aiticket.ai.embedding;

import com.example.aiticket.config.AiProviderProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Component
public class SiliconFlowEmbeddingClient implements EmbeddingClient {
    private final AiProviderProperties properties;
    private final RestClient restClient;

    public SiliconFlowEmbeddingClient(AiProviderProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.getEmbedding().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getEmbedding().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public EmbeddingResult embed(String text) {
        List<EmbeddingResult> results = embedBatch(List.of(text));
        return results.getFirst();
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be empty");
        }
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("texts must not contain blank values");
        }

        EmbeddingRequest request = new EmbeddingRequest(
                properties.getEmbedding().getModel(),
                texts,
                properties.getEmbedding().getDimensions(),
                "float"
        );

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().size() != texts.size()) {
            throw new IllegalStateException("embedding response size does not match request size");
        }

        return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(item -> new EmbeddingResult(
                        properties.getEmbedding().getModel(),
                        properties.getEmbedding().getDimensions(),
                        item.embedding()
                ))
                .toList();
    }

    private record EmbeddingRequest(String model, List<String> input, Integer dimensions, String encodingFormat) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(int index, List<Float> embedding) {
    }
}
```

- [x] **Step 4: Fix JSON property naming** ⭐

If the focused test fails because `encodingFormat` serializes as `encodingFormat` instead of `encoding_format`, add Jackson annotation:

```java
import com.fasterxml.jackson.annotation.JsonProperty;
```

```java
    private record EmbeddingRequest(
            String model,
            List<String> input,
            Integer dimensions,
            @JsonProperty("encoding_format") String encodingFormat
    ) {
    }
```

- [x] **Step 5: Update provider-neutral result test fixture** ⭐

Change `backend/src/test/java/com/example/aiticket/ai/embedding/EmbeddingResultTest.java`:

```java
assertThatThrownBy(() -> new EmbeddingResult("Qwen/Qwen3-Embedding-8B", 1024, List.of(0.1f, 0.2f)))
```

- [x] **Step 6: Run focused embedding tests** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -Dtest=SiliconFlowEmbeddingClientTest,EmbeddingResultTest test
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 7: Search for old provider class references** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
rg -n "AliyunBailianEmbeddingClient|aliyun-bailian|dashscope|text-embedding-v3|阿里百炼|Aliyun Bailian" backend
```

Expected:

```text
```

No output from `backend` after the code and config switch.

- [x] **Step 8: Commit provider adapter replacement** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add backend/src/main/java/com/example/aiticket/ai/embedding backend/src/test/java/com/example/aiticket/ai/embedding
git commit -m "feat: switch embedding client to siliconflow"
```

---

## Task 3: Synchronize Project Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/spikes/oracle-vector-spike.md`
- Modify: `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
- Modify: `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-system-design.md`
- Modify: `docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md`
- Modify: `沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
- Modify: `沟通材料/2026-06-19-ai-knowledge-ticket-system-design.md`

- [x] **Step 1: Replace first-version provider descriptions** ⭐

Replace user-facing references:

```text
阿里百炼 `text-embedding-v3`
```

with:

```text
硅基流动 `Qwen/Qwen3-Embedding-8B`
```

Replace English references:

```text
Aliyun Bailian `text-embedding-v3`
```

with:

```text
SiliconFlow `Qwen/Qwen3-Embedding-8B`
```

- [x] **Step 2: Replace default config snippets** ⭐

Use this embedding config wherever the docs show provider defaults:

```yaml
  embedding:
    provider: siliconflow
    base-url: https://api.siliconflow.cn/v1
    api-key: ${AI_EMBEDDING_API_KEY}
    model: Qwen/Qwen3-Embedding-8B
    dimensions: 1024
```

If the snippet is copied from `application.yml`, use placeholder-preserving defaults:

```yaml
  embedding:
    provider: siliconflow
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.siliconflow.cn/v1}
    api-key: ${AI_EMBEDDING_API_KEY:}
    model: ${AI_EMBEDDING_MODEL:Qwen/Qwen3-Embedding-8B}
    dimensions: ${AI_EMBEDDING_DIMENSIONS:1024}
```

- [x] **Step 3: Preserve the verified Oracle vector decision** ⭐

Keep this conclusion in every project/spec document:

```text
Oracle 向量字段继续使用 `VECTOR(1024, FLOAT32)`，第一版采用精确 `VECTOR_DISTANCE(..., COSINE)` Top-K 检索，暂不引入 HNSW 或 IVF 近似向量索引。
```

- [x] **Step 4: Update live verification checklist wording** ⭐

In `docs/spikes/oracle-vector-spike.md`, replace the live checklist with:

```markdown
- [ ] SiliconFlow single embedding call returned 1024 dimensions.
- [ ] SiliconFlow batch embedding call returned 1024 dimensions for each input.
```

Do not check these boxes until Task 5 actually runs against SiliconFlow.

- [x] **Step 5: Add correction note to the old foundation plan** ⭐

Near the top of `docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md`, add:

```markdown
> Update on 2026-06-19: The Oracle vector spike remains valid, but the first-version Embedding provider changed from Aliyun Bailian `text-embedding-v3` to SiliconFlow `Qwen/Qwen3-Embedding-8B`. Oracle vector dimensions stay at `1024`.
```

Do not rewrite every historical task in that old plan unless it would confuse current execution.

- [x] **Step 6: Scan docs for stale first-version provider references** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
rg -n "阿里百炼|Aliyun Bailian|aliyun-bailian|dashscope|text-embedding-v3" README.md docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-system-design.md docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md docs/spikes/oracle-vector-spike.md 沟通材料
```

Expected:

```text
```

No stale first-version provider references outside historical context intentionally preserved in the SiliconFlow design spec or correction notes.

- [x] **Step 7: Commit documentation synchronization** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add README.md docs 沟通材料
git commit -m "docs: align embedding provider with siliconflow"
```

---

## Task 4: Run Full Local Verification

**Files:**
- Read only unless failures require fixes.

- [x] **Step 1: Run the full backend test suite** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 2: Check Docker service status** ⭐

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
docker compose ps
```

Expected:

```text
oracle
redis
```

Both services should be running or healthy before live Oracle regression. If Docker access is blocked by sandboxing, rerun with escalation.

- [x] **Step 3: Start backend for regression only if needed** ⭐

If no backend is already running on port `8080`, run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

Expected startup evidence:

```text
Started AiTicketBackendApplication
```

- [x] **Step 4: Re-run existing manual vector spike endpoints** ⭐

Insert a deterministic 1024-dimension vector:

```bash
curl -s http://localhost:8080/api/spike/vector \
  -H 'Content-Type: application/json' \
  -d '{"title":"siliconflow-regression-manual","content":"manual vector regression after provider switch","embedding":[0.001,0.001,0.001]}'
```

Use the existing endpoint payload shape actually supported by the controller. If the current endpoint requires all 1024 values explicitly, generate the vector in a temporary shell command without committing it.

Search:

```bash
curl -s http://localhost:8080/api/spike/vector/search \
  -H 'Content-Type: application/json' \
  -d '{"embedding":[0.001,0.001,0.001],"topK":3}'
```

Expected:

```text
siliconflow-regression-manual
```

- [x] **Step 5: Fix failures before continuing** ⭐

If a test or regression command fails, inspect the exact failure first and make the smallest code or documentation correction that explains the failure. Re-run the same command until it passes.

---

## Task 5: Live SiliconFlow Verification

**Files:**
- Modify: `docs/spikes/oracle-vector-spike.md`
- Modify: `docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md`
- Optionally modify: this plan file to mark live steps complete with `⭐`.

- [ ] **Step 1: Check API key presence without printing it**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/backend
if [ -n "$AI_EMBEDDING_API_KEY" ]; then echo SET; else echo MISSING; fi
```

Expected:

```text
SET
```

If the result is `MISSING`, stop live verification and ask the user to export `AI_EMBEDDING_API_KEY` in the environment visible to Codex. Do not ask the user to paste the key into chat.

- [ ] **Step 2: Run a real single-text Embedding call through the backend client**

Use a temporary non-committed test or command that loads the Spring context with the configured `AI_EMBEDDING_API_KEY` and calls:

```java
EmbeddingResult result = embeddingClient.embed("硅基流动单条向量验证");
assertThat(result.model()).isEqualTo("Qwen/Qwen3-Embedding-8B");
assertThat(result.dimensions()).isEqualTo(1024);
assertThat(result.vector()).hasSize(1024);
```

Expected:

```text
single embedding dimensions=1024
```

Do not print the API key or the full vector.

- [ ] **Step 3: Run a real batch Embedding call through the backend client**

Use the same temporary runner to call:

```java
List<EmbeddingResult> results = embeddingClient.embedBatch(List.of(
        "硅基流动批量向量验证一",
        "硅基流动批量向量验证二"
));
assertThat(results).hasSize(2);
assertThat(results).allSatisfy(result -> assertThat(result.vector()).hasSize(1024));
```

Expected:

```text
batch embedding count=2 dimensions=1024,1024
```

Do not print full vectors.

- [ ] **Step 4: Insert a real SiliconFlow vector into Oracle**

With the backend running and Docker Oracle healthy, send a vector generated by the real Embedding call to `/api/spike/vector`.

Expected:

```text
created or inserted record id returned by the endpoint
```

Record only the fact that the vector length was `1024`; do not paste the vector into docs.

- [ ] **Step 5: Search Oracle with a real SiliconFlow query vector**

Generate a query vector using SiliconFlow and call `/api/spike/vector/search`.

Expected:

```text
The inserted SiliconFlow verification row appears in top-k results.
```

- [ ] **Step 6: Record live verification results**

Update `docs/spikes/oracle-vector-spike.md`:

```markdown
- [x] SiliconFlow single embedding call returned 1024 dimensions. ⭐
- [x] SiliconFlow batch embedding call returned 1024 dimensions for each input. ⭐
```

Add a short dated note:

```markdown
2026-06-19 update: SiliconFlow `Qwen/Qwen3-Embedding-8B` live single and batch calls returned 1024-dimensional vectors, and a real provider vector was inserted into Oracle `VECTOR(1024, FLOAT32)` and retrieved with `VECTOR_DISTANCE(..., COSINE)`.
```

- [ ] **Step 7: Commit live verification docs**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git add docs/spikes/oracle-vector-spike.md docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md docs/superpowers/plans/2026-06-19-siliconflow-embedding-provider-implementation-plan.md
git commit -m "docs: record siliconflow embedding verification"
```

---

## Task 6: Final Review

**Files:**
- Read only unless review finds a specific issue.

- [ ] **Step 1: Inspect final diff**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
git status --short
git diff --stat HEAD~3..HEAD
git diff HEAD~3..HEAD -- backend/src/main/resources/application.yml backend/src/main/java/com/example/aiticket/ai/embedding backend/src/test/java/com/example/aiticket
```

Expected:

```text
Only SiliconFlow provider, tests, docs, and plan tracking changed.
```

- [ ] **Step 2: Confirm no secrets were committed**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
rg -n "sk-|Bearer [A-Za-z0-9]|AI_EMBEDDING_API_KEY=.+" .
```

Expected:

```text
```

No real key appears in tracked files. Placeholder references like `${AI_EMBEDDING_API_KEY:}` are allowed.

- [ ] **Step 3: Confirm old backend provider references are gone**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
rg -n "AliyunBailianEmbeddingClient|aliyun-bailian|dashscope|text-embedding-v3|阿里百炼|Aliyun Bailian" backend
```

Expected:

```text
```

- [ ] **Step 4: Confirm implementation plan tracking**

Run:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计
rg -n "^- \\[x\\].*⭐" docs/superpowers/plans/2026-06-19-siliconflow-embedding-provider-implementation-plan.md
```

Expected:

```text
Each completed step has a star.
```

---

## Self-Review

Spec coverage:

- SiliconFlow provider defaults are covered in Task 1.
- `SiliconFlowEmbeddingClient` replacement is covered in Task 2.
- `encoding_format: float`, batch input, bearer authorization, response sorting, and dimension validation are covered in Task 2 tests.
- Documentation synchronization is covered in Task 3.
- Full local Maven verification and Oracle regression are covered in Task 4.
- Live SiliconFlow verification is covered in Task 5 and explicitly avoids checking boxes without a real key.
- Secret handling is covered in Task 5 and Task 6.
- Extensibility is preserved by keeping `EmbeddingClient`, `EmbeddingResult`, `AiProviderProperties`, and `ai.embedding.*` stable while swapping only the concrete adapter.

Placeholder scan:

- No `TBD`, `TODO`, or unexpanded “add tests” placeholders remain.
- All code-changing steps include concrete snippets.

Type consistency:

- Provider class is consistently named `SiliconFlowEmbeddingClient`.
- Model is consistently `Qwen/Qwen3-Embedding-8B`.
- Default dimensions are consistently `1024`.
- The request JSON field is consistently `encoding_format` via `@JsonProperty`.
