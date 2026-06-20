# Phase 20 V1 Plan Status Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Bring the original V1 project plan up to date with the implemented Phase 9-19 work so the written plan no longer contradicts the current system.

**Architecture:** Keep this as a documentation consolidation slice. A documentation coverage test protects the main V1 plan from regressing to outdated status language, then the plan itself is updated to reflect implemented frontend pages, SSE, file upload, demo corpus, live rehearsal materials, and remaining non-V1 scope.

**Tech Stack:** Markdown, Java 21, JUnit 5, AssertJ.

---

## File Structure

```text
backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java
docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md
docs/superpowers/plans/2026-06-20-phase-20-v1-plan-status-consolidation-implementation-plan.md
```

## Task 1: Main Plan Status Guard ⭐

**Files:**
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`

- [x] **Step 1: Write the failing status coverage test**

Add a test that reads `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md` and asserts:

```text
contains Phase 19
contains 前端 RAG chat
contains 工单列表/详情
contains 知识库管理
contains 系统管理
contains 文件上传
contains SSE
contains live-provider rehearsal
does not contain 下一步推进剩余前端业务页
```

- [x] **Step 2: Run focused docs test to verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: fail because the main V1 project plan still says remaining frontend business pages need to be connected.

## Task 2: Consolidate Original V1 Project Plan ⭐

**Files:**
- Modify: `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`

- [x] **Step 1: Replace the outdated progress paragraph**

Update the progress paragraph around section 12 so it says the V1 implementation now covers:

```text
backend foundation, RBAC/JWT, Oracle vector spike, SiliconFlow embeddings, knowledge retrieval, RAG ask, SSE stream, ticket workflow, ticket comments, admin statistics, frontend shell, homepage, RAG chat frontend, ticket list/detail, knowledge management, system management, demo corpus, file upload, route smoke alignment, and Phase 19 live-provider rehearsal evidence.
```

- [x] **Step 2: Replace the outdated next-step paragraph**

Replace `下一步推进剩余前端业务页...` with a current statement:

```text
下一步主要是执行真实环境 live-provider rehearsal、补充 RAG 实测结果、整理系统截图和论文正文，不再把 V1 前端业务页作为未完成主线。
```

- [x] **Step 3: Update the phase checklist**

Add checked bullets for:

```text
第九至第十三阶段前端业务闭环已完成
第十四至第十八阶段演示、语料、路由、SSE 前端和文件上传增强已完成
第十九阶段 live-provider rehearsal 证据链已完成
```

- [x] **Step 4: Update phase-specific current progress notes**

Update section 12.5, 12.6, and 12.7 current-progress notes so they no longer describe frontend pages as incomplete. Keep explicit future scope for star rating, ECharts-level visualization, complex file parsing, SLA, approval, and automatic assignment.

## Task 3: Verification and Commit ⭐

**Files:**
- Modify: `docs/superpowers/plans/2026-06-20-phase-20-v1-plan-status-consolidation-implementation-plan.md`

- [x] **Step 1: Run focused docs test to verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: pass.

- [x] **Step 2: Run full backend tests**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Expected: pass.

- [x] **Step 3: Mark completed plan tasks**

Append `⭐` to each completed task heading and change completed steps to `- [x]`.

- [x] **Step 4: Commit Phase 20 slice**

Run:

```bash
git add backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md docs/superpowers/plans/2026-06-20-phase-20-v1-plan-status-consolidation-implementation-plan.md
git commit -m "docs: consolidate v1 plan status"
```
