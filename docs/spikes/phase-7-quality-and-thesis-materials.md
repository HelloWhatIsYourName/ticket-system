# Phase 7 Quality and Thesis Materials Verification

Date: 2026-06-20

## Scope

Phase 7 turns the implemented backend slices into repeatable verification and thesis-ready evidence:

- reusable backend smoke script
- structured RAG evaluation set and scoring guide
- V1 acceptance matrix
- 10-15 minute demo runbook
- documentation coverage tests that keep these materials present and aligned with core modules

This phase does not add new product workflow capability. It prepares evidence for testing, optimization, demo, and thesis writing.

## Automated Tests

Focused documentation and evaluation tests:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest,RagEvaluationSetTest test
```

Result:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend suite:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
```

Result:

```text
Tests run: 96, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Smoke Script

Created:

```text
tools/smoke/phase7-backend-smoke.sh
```

Syntax verification:

```bash
bash -n tools/smoke/phase7-backend-smoke.sh
```

Result: exit code 0.

Runtime usage:

```bash
BASE_URL=http://127.0.0.1:8080 tools/smoke/phase7-backend-smoke.sh
```

The script logs in as `admin`, `user`, and `agent`, prints only `token:redacted`, and checks the core backend path:

- auth current user
- knowledge text creation
- knowledge search
- RAG ask
- ticket creation from AI session
- admin assignment
- user and agent ticket lists
- admin statistics
- admin user list
- anonymous `401`
- ordinary user admin denial `403`

Runtime smoke was not rerun in this final Task 4 pass because it depends on the backend, Oracle, Redis, and AI provider/mock provider being online together. Phase 6 already live-smoke verified the admin/statistics slice; this phase adds a reusable script for the full path.

## RAG Evaluation Set

Created:

```text
docs/evaluation/rag-evaluation-set.json
docs/evaluation/rag-evaluation-set.md
```

Dataset summary:

- 20 Chinese enterprise-service questions
- categories include 账号与登录, 权限申请, 网络与 VPN, 设备支持, 报销与财务, 办公流程, 未知或需人工
- 5 cases require human transfer
- scoring guide defines 检索命中率, 回答有用率, 误转工单率, and 应转未转率

This gives the thesis a concrete evaluation set without pretending the small dataset is a statistically rigorous benchmark.

## Acceptance and Demo Materials

Created:

```text
docs/acceptance/v1-acceptance-checklist.md
docs/demo/v1-demo-runbook.md
```

The acceptance checklist maps the 14 project-plan criteria to backend implementation, automated tests, live/manual evidence, and remaining frontend or hardening notes.

The demo runbook provides a 10-15 minute sequence covering:

- RBAC login and denial checks
- 知识库 document creation and search
- RAG question with citations
- transfer to 工单
- admin assignment
- agent processing
- user feedback and closure
- admin 统计 and management APIs
- Redis queue/design evidence

## Remaining Scope

The backend evidence and thesis-supporting materials are in place. Remaining work is presentation-layer and final-writing oriented:

- Vue3 + Element Plus + ECharts frontend integration
- final demo corpus loading
- actual RAG metric values after running the 20-case evaluation set against the final corpus
- optional SSE endpoint and multipart upload hardening if required by final acceptance interpretation
