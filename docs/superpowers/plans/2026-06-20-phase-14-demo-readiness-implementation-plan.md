# Phase 14 Demo Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Add a visible frontend demo guide and update defense documentation so the full V1 loop can be demonstrated consistently.

**Architecture:** Add `/app/demo` as a lightweight route inside the existing app shell. The page is static, testable, and links to the already implemented frontend workflows: knowledge management, RAG chat, tickets, dashboard, and system admin. Update the existing demo runbook and acceptance checklist to reflect that frontend integration is now implemented.

**Tech Stack:** Vue 3, TypeScript, Vite, Vitest, Vue Test Utils, Vue Router.

---

## File Structure

```text
frontend/src/views/demo/DemoGuideView.vue
frontend/src/views/demo/DemoGuideView.spec.ts
frontend/src/router/index.ts
frontend/src/layouts/AppShell.vue
frontend/src/styles/main.css
docs/demo/v1-demo-runbook.md
docs/acceptance/v1-acceptance-checklist.md
docs/superpowers/plans/2026-06-20-phase-14-demo-readiness-implementation-plan.md
```

## Task 1: Demo Guide Frontend Page ⭐

**Files:**
- Create: `frontend/src/views/demo/DemoGuideView.vue`
- Create: `frontend/src/views/demo/DemoGuideView.spec.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/AppShell.vue`
- Modify: `frontend/src/styles/main.css`

- [x] **Step 1: Write failing page test**

Create `DemoGuideView.spec.ts` asserting the page renders `答辩演示导览`, `知识录入`, `AI 问答`, `转工单`, `处理工单`, `统计看板`, and links to `/app/knowledge`, `/app/ai/chat`, `/app/tickets/my`, `/app/admin/dashboard`, `/app/system`.

- [x] **Step 2: Run page test to verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/views/demo/DemoGuideView.spec.ts
```

Expected: fail because `DemoGuideView.vue` does not exist.

- [x] **Step 3: Implement demo guide page**

Create a concise operational guide with seven steps:

1. 登录与权限恢复.
2. 知识录入.
3. 检索测试.
4. AI 问答.
5. 转工单.
6. 工单处理.
7. 统计与系统管理.

- [x] **Step 4: Add route and fallback menu**

Add `/app/demo` to `router/index.ts`. Add `演示导览` to `AppShell.vue` fallback menu as the first app item.

- [x] **Step 5: Run page test to verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/views/demo/DemoGuideView.spec.ts
```

Expected: pass.

## Task 2: Demo Documentation Sync ⭐

**Files:**
- Modify: `docs/demo/v1-demo-runbook.md`
- Modify: `docs/acceptance/v1-acceptance-checklist.md`

- [x] **Step 1: Update demo runbook**

Revise the runbook so it includes frontend startup, `/app/demo`, and implemented frontend pages instead of saying frontend is remaining work.

- [x] **Step 2: Update acceptance checklist remaining notes**

Mark frontend dynamic menus, citation display, ticket workflow UI, dashboard UI, knowledge UI, and system admin UI as implemented in the frontend slices.

## Task 3: Verification and Plan Marking ⭐

**Files:**
- Modify: `docs/superpowers/plans/2026-06-20-phase-14-demo-readiness-implementation-plan.md`

- [x] **Step 1: Run full frontend test suite**

Run:

```bash
cd frontend
npm run test
```

Expected: all frontend tests pass.

- [x] **Step 2: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build complete successfully.

- [x] **Step 3: Mark completed plan tasks**

For each implemented and verified task heading, append `⭐` and change its steps from `- [ ]` to `- [x]`.

- [x] **Step 4: Commit Phase 14 slice**

Run:

```bash
git add frontend docs/demo/v1-demo-runbook.md docs/acceptance/v1-acceptance-checklist.md docs/superpowers/plans/2026-06-20-phase-14-demo-readiness-implementation-plan.md
git commit -m "feat: add demo readiness guide"
```
