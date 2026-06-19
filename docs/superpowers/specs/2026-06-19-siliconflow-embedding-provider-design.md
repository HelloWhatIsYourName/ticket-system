# 硅基流动 Embedding 供应商替换设计

## 1. 背景

项目第一阶段已经完成 Oracle 23ai `VECTOR(1024, FLOAT32)` 的本地写入和 Top-K 检索验证。原计划第一版 Embedding 供应商使用阿里百炼 `text-embedding-v3`，但当前环境缺少阿里百炼 API Key，且项目后续决定改用硅基流动提供的 Embedding API。

本设计用于把第一版 Embedding 供应商从阿里百炼调整为硅基流动，同时尽量保留已经验证通过的 Oracle 1024 维向量存储方案。

## 2. 决策

第一版 Embedding 供应商改为硅基流动。

推荐模型：

```text
Qwen/Qwen3-Embedding-8B
```

向量维度继续保持：

```text
1024
```

Oracle 字段继续保持：

```sql
embedding VECTOR(1024, FLOAT32)
```

选择该方案的原因：

1. 当前 Oracle Vector Spike 已验证 1024 维向量可以通过 Spring Boot、MyBatis 和 Oracle 23ai 完成写入与检索。
2. 保持 1024 维可以避免修改 `V1__vector_spike.sql` 和后续知识片段表的向量字段设计。
3. 硅基流动 Embeddings API 使用 OpenAI-compatible 风格，请求和响应结构与现有客户端实现接近。
4. 硅基流动官方文档说明 `dimensions` 参数支持 Qwen/Qwen3 Embedding 系列，因此可以把 `Qwen/Qwen3-Embedding-8B` 配置为 1024 维。

## 3. 范围

### 3.1 本次替换实现

1. 更新默认配置：
   - `ai.embedding.provider=siliconflow`
   - `ai.embedding.base-url=https://api.siliconflow.com/v1`
   - `ai.embedding.model=Qwen/Qwen3-Embedding-8B`
   - `ai.embedding.dimensions=1024`
2. 将 `AliyunBailianEmbeddingClient` 替换为 `SiliconFlowEmbeddingClient`。
3. 保留 `EmbeddingClient` 接口和 `EmbeddingResult` 结果对象。
4. 继续使用 `Authorization: Bearer <apiKey>`。
5. 调用 `/embeddings` 接口。
6. 请求体支持批量文本：

```json
{
  "model": "Qwen/Qwen3-Embedding-8B",
  "input": ["文本1", "文本2"],
  "dimensions": 1024,
  "encoding_format": "float"
}
```

7. 响应解析继续按 `data[].index` 排序，并读取 `data[].embedding`。
8. 更新单元测试中 provider、base URL、model 和维度断言。
9. 更新项目计划书、系统设计、README、spike report 和 foundation implementation plan 中的供应商描述。
10. 将原阿里百炼 live 验证项替换为硅基流动 live 验证项。

### 3.2 本次不实现

1. 不实现多个 Embedding 供应商同时注册和运行时切换。
2. 不实现模型路由。
3. 不实现模型配置管理后台。
4. 不修改 Oracle `VECTOR(1024, FLOAT32)` 字段。
5. 不修改已验证通过的 `VectorSpikeMapper.xml` 向量写入和查询 SQL。
6. 不把 API Key 写入仓库。

多供应商切换能力保留在配置结构中，后续可通过条件 Bean 或 provider factory 实现。

## 4. 配置设计

`backend/src/main/resources/application.yml` 中的默认配置调整为：

```yaml
ai:
  embedding:
    provider: siliconflow
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.siliconflow.com/v1}
    api-key: ${AI_EMBEDDING_API_KEY:}
    model: ${AI_EMBEDDING_MODEL:Qwen/Qwen3-Embedding-8B}
    dimensions: ${AI_EMBEDDING_DIMENSIONS:1024}
```

本地真实调用时通过环境变量传入：

```bash
export AI_EMBEDDING_API_KEY=<siliconflow-api-key>
```

不在 `.env.example` 或 `application.yml` 中写真实密钥。

## 5. 客户端设计

`SiliconFlowEmbeddingClient` 实现 `EmbeddingClient`。

构造逻辑：

1. 从 `AiProviderProperties` 读取 Embedding 配置。
2. 使用 `RestClient.Builder` 创建客户端。
3. 设置 `baseUrl` 为 `ai.embedding.base-url`。
4. 设置默认请求头 `Authorization: Bearer <apiKey>`。
5. 设置默认请求头 `Content-Type: application/json`。

单条调用：

1. `embed(String text)` 校验文本非空。
2. 调用 `embedBatch(List.of(text))`。
3. 返回第一条结果。

批量调用：

1. `embedBatch(List<String> texts)` 校验列表非空，且每条文本非空。
2. 请求体包含 `model`、`input`、`dimensions` 和 `encoding_format`。
3. 请求 `/embeddings`。
4. 若响应为空、`data` 为空或数量与输入数量不一致，则抛出 `IllegalStateException`。
5. 按 `data[].index` 排序后转换为 `EmbeddingResult`。
6. `EmbeddingResult` 继续校验返回向量长度等于配置维度。

## 6. 文档更新

需要更新的文档包括：

1. `README.md`
2. `docs/spikes/oracle-vector-spike.md`
3. `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
4. `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-system-design.md`
5. `docs/superpowers/plans/2026-06-19-foundation-vector-spike-implementation-plan.md`
6. `沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
7. `沟通材料/2026-06-19-ai-knowledge-ticket-system-design.md`

文档调整原则：

1. 把第一版 Embedding 供应商统一改为硅基流动。
2. 把模型统一改为 `Qwen/Qwen3-Embedding-8B`。
3. 保留 Oracle `VECTOR(1024, FLOAT32)`。
4. 保留已经完成的 Oracle Vector 本地手工验证结论。
5. 把未完成的阿里百炼 live 验证项改为硅基流动 live 验证项。

## 7. 验证策略

### 7.1 本地测试

运行：

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
```

预期：

```text
BUILD SUCCESS
```

### 7.2 本地 Oracle Vector 回归验证

保持 Docker Oracle 和 Redis 运行：

```bash
docker compose ps
```

启动后端：

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

继续使用 `/api/spike/vector` 和 `/api/spike/vector/search` 验证 1024 维向量写入和查询。

### 7.3 硅基流动 live 验证

在用户完成充值并配置 `AI_EMBEDDING_API_KEY` 后，执行真实 API 验证：

1. 单条文本 Embedding 返回 1024 维。
2. 批量文本 Embedding 每条都返回 1024 维。
3. 将真实 Embedding 写入 Oracle `VECTOR(1024, FLOAT32)`。
4. 用真实 query Embedding 执行 Top-K 检索。

live 验证通过后，再更新 `docs/spikes/oracle-vector-spike.md` 和 foundation plan 中对应 checkbox，并加 `⭐`。

## 8. 验收标准

1. 代码中默认 Embedding provider 为 `siliconflow`。
2. 默认 base URL 为 `https://api.siliconflow.com/v1`。
3. 默认模型为 `Qwen/Qwen3-Embedding-8B`。
4. 默认维度仍为 `1024`。
5. 项目文档不再把阿里百炼描述为第一版 Embedding 供应商。
6. `EmbeddingClient` 抽象不变，后续知识库模块不依赖具体供应商类名。
7. Java 21 下 `mvn test` 通过。
8. 未配置硅基流动 API Key 时，不伪造 live 验证结果。
9. 配置 API Key 后，单条和批量 Embedding live 验证都能返回 1024 维。
