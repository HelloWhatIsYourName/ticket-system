# AI Knowledge Ticket System

AI Knowledge Ticket System 是一个面向企业知识库问答和工单协同处理的毕业设计项目。当前成品已经覆盖知识库向量检索、RAG 问答、AI 问答转工单、RBAC 权限控制、工单全流程、管理员统计、智能分派推荐和优先级 SLA 展示。

最终答辩手测路径见：

- [docs/demo/final-defense-runbook.md](docs/demo/final-defense-runbook.md)

## Implemented Features

| Area | Capability |
| --- | --- |
| 权限与菜单 | Spring Security + JWT + RBAC，按角色加载菜单和权限 |
| 知识库 | 文本录入、`.txt/.md` 文件上传、Oracle 23ai `VECTOR(1024, FLOAT32)` 检索 |
| AI 问答 | OpenAI-compatible Chat，SiliconFlow Embedding，RAG 引用来源，SSE 流式返回和普通 HTTP 回退 |
| 工单协同 | AI 会话转工单，用户/坐席/管理员按权限处理状态流转 |
| 智能分派 | 管理员详情页显示最低负载坐席推荐，点击后填充现有分配下拉框 |
| SLA | 根据优先级生成 `deadlineAt`，列表和详情展示 `ON_TRACK`、`DUE_SOON`、`OVERDUE`、`COMPLETED` |
| 管理后台 | 工单统计、知识库数量、AI 问答指标、用户/角色/权限管理 |

## Local Startup

Worktree used for current development:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31
```

Start dependencies:

```bash
docker compose up -d
docker compose ps
```

Start backend with Java 21 and local secret env files:

```bash
cd backend

set -a
source /private/tmp/ai-ticket-secrets/siliconflow.env
source /private/tmp/ai-ticket-secrets/deepseek.env
set +a

JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn spring-boot:run
```

Do not use `/usr/libexec/java_home -v 21` on this machine; it may resolve to Java 17.

Start frontend:

```bash
cd frontend
npm run dev -- --host 127.0.0.1 --port 5175
```

Open:

```text
http://127.0.0.1:5175
```

## Demo Accounts

| Role | Username | Password |
| --- | --- | --- |
| 管理员 | `admin` | `Admin_123456` |
| 普通用户 | `user` | `Admin_123456` |
| 坐席 | `agent` | `Admin_123456` |

Some recovered demo databases also include `agent2 / Admin_123456`. The assignment recommendation panel may choose `agent2` when it has the lowest active workload. During defense, follow the assignee displayed by the recommendation panel, or manually choose `agent` if the script must use only baseline accounts.

## Verification

Backend tests:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn test
```

Frontend tests and build:

```bash
cd frontend
npm run test
npm run build
```

Acceptance evidence:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31
FRONTEND_BASE_URL=http://127.0.0.1:5175 \
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
tools/smoke/phase31-acceptance-evidence.sh
```

Expected checks:

```text
Backend smoke                  PASS
Frontend dev smoke             PASS
Frontend tests                 PASS
Frontend build                 PASS
Backend documentation coverage PASS
```

The smoke and acceptance scripts redact tokens. Never commit API keys, JWTs, or local secret files.

## Key Documents

- [Final defense demo runbook](docs/demo/final-defense-runbook.md)
- [V1 acceptance checklist](docs/acceptance/v1-acceptance-checklist.md)
- [Phase 39/40 design](docs/superpowers/specs/2026-06-23-phase-39-40-assignment-sla-design.md)
- [Phase 39/40 implementation plan](docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md)
