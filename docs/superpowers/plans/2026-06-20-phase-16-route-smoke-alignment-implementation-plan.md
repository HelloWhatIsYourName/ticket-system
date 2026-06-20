# Phase 16 Route and Smoke Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Align demo verification scripts, backend RBAC menu seeds, and frontend app routes so fresh and migrated environments navigate to implemented V1 screens.

**Architecture:** Keep endpoint drift covered by documentation tests that read the smoke script. Keep RBAC menu paths in database migrations because `UserSecurityMapper` returns `sys_menu.route_path` directly to the frontend. Add `/app/tickets/assigned` as a lightweight reuse of the ticket list page so the agent-workbench menu points to a real implemented route instead of a placeholder.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Flyway SQL migrations, Vue 3, Vue Router, Vitest.

---

## File Structure

```text
backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java
backend/src/test/java/com/example/aiticket/docs/MenuRouteAlignmentTest.java
backend/src/main/resources/db/migration/V2__auth_rbac.sql
backend/src/main/resources/db/migration/V6__frontend_route_alignment.sql
tools/smoke/phase7-backend-smoke.sh
frontend/src/router/index.ts
frontend/src/router/index.spec.ts
frontend/src/layouts/AppShell.vue
frontend/src/views/tickets/TicketListView.vue
frontend/src/views/tickets/TicketListView.spec.ts
docs/superpowers/plans/2026-06-20-phase-16-route-smoke-alignment-implementation-plan.md
```

## Task 1: Smoke Script Endpoint Drift ⭐

**Files:**
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`
- Modify: `tools/smoke/phase7-backend-smoke.sh`

- [x] **Step 1: Write failing documentation expectation**

Change `DocumentationCoverageTest.phase7SmokeScriptCoversCoreBackendEndpointsWithoutPrintingTokens` so it expects the implemented knowledge endpoints:

```java
assertThat(script).contains("/api/kb/documents/text");
assertThat(script).contains("/api/kb/search");
assertThat(script).doesNotContain("/api/knowledge/documents/text");
assertThat(script).doesNotContain("/api/knowledge/search");
```

- [x] **Step 2: Run focused test to verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: fail because `tools/smoke/phase7-backend-smoke.sh` still uses `/api/knowledge/documents/text` and `/api/knowledge/search`.

- [x] **Step 3: Update smoke script paths**

In `tools/smoke/phase7-backend-smoke.sh`, replace:

```bash
/api/knowledge/documents/text
/api/knowledge/search
```

with:

```bash
/api/kb/documents/text
/api/kb/search
```

- [x] **Step 4: Run focused test to verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: pass.

## Task 2: Backend Menu Route Alignment ⭐

**Files:**
- Create: `backend/src/test/java/com/example/aiticket/docs/MenuRouteAlignmentTest.java`
- Modify: `backend/src/main/resources/db/migration/V2__auth_rbac.sql`
- Create: `backend/src/main/resources/db/migration/V6__frontend_route_alignment.sql`

- [x] **Step 1: Write failing menu route test**

Create `MenuRouteAlignmentTest` that reads `V2__auth_rbac.sql` and asserts fresh database seeds contain:

```text
'/app/ai/chat'
'/app/tickets/my'
'/app/tickets/assigned'
'/app/knowledge'
'/app/system'
'/app/admin/dashboard'
```

The same test must assert the seed does not contain the old frontend paths `'/chat'`, `'/tickets'`, `'/agent/tickets'`, `'/admin/knowledge'`, `'/admin/users'`, or `'/admin/dashboard'`.

- [x] **Step 2: Run focused test to verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=MenuRouteAlignmentTest test
```

Expected: fail because `V2__auth_rbac.sql` still seeds old route paths.

- [x] **Step 3: Update fresh seed routes**

Update `V2__auth_rbac.sql` menu rows:

```text
chat -> /app/ai/chat
tickets -> /app/tickets/my
agent-workbench -> /app/tickets/assigned
knowledge -> /app/knowledge
users -> /app/system
dashboard -> /app/admin/dashboard
```

- [x] **Step 4: Add migrated database route correction**

Create `V6__frontend_route_alignment.sql` with `UPDATE sys_menu SET route_path = ... WHERE menu_code = ...` statements for the six menu codes above.

- [x] **Step 5: Run focused test to verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=MenuRouteAlignmentTest test
```

Expected: pass.

## Task 3: Assigned Ticket Frontend Route ⭐

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/index.spec.ts`
- Modify: `frontend/src/layouts/AppShell.vue`
- Modify: `frontend/src/views/tickets/TicketListView.vue`
- Modify: `frontend/src/views/tickets/TicketListView.spec.ts`

- [x] **Step 1: Write failing frontend route and list tests**

Extend router tests to assert `/app` children include `tickets/assigned`. Extend `TicketListView.spec.ts` so mounting the view under route name `assigned-tickets` calls `listAssignedTickets()` and renders `分配给我的工单`.

- [x] **Step 2: Run focused frontend tests to verify RED**

Run:

```bash
cd frontend
npm run test -- src/router/index.spec.ts src/views/tickets/TicketListView.spec.ts
```

Expected: fail because the route is missing and `TicketListView` always calls `listMyTickets()`.

- [x] **Step 3: Add assigned route and fallback menu**

Add the `/app/tickets/assigned` child route before `/app/tickets/:ticketId`. Add fallback menu item `{ code: 'assigned-tickets', name: '分配给我', path: '/app/tickets/assigned' }` after `我的工单`.

- [x] **Step 4: Reuse TicketListView by route mode**

In `TicketListView.vue`, use `useRoute()` to detect `route.name === 'assigned-tickets'`; call `listAssignedTickets()` for assigned mode and keep `listMyTickets()` for my tickets mode. Render heading and empty/table labels from the mode.

- [x] **Step 5: Run focused frontend tests to verify GREEN**

Run:

```bash
cd frontend
npm run test -- src/router/index.spec.ts src/views/tickets/TicketListView.spec.ts
```

Expected: pass.

## Task 4: Full Verification and Plan Marking ⭐

**Files:**
- Modify: `docs/superpowers/plans/2026-06-20-phase-16-route-smoke-alignment-implementation-plan.md`

- [x] **Step 1: Run backend focused docs tests**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest,MenuRouteAlignmentTest test
```

Expected: pass.

- [x] **Step 2: Run frontend test suite**

Run:

```bash
cd frontend
npm run test
```

Expected: pass.

- [x] **Step 3: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: pass.

- [x] **Step 4: Mark completed plan tasks**

Append `⭐` to each completed task heading and change completed steps to `- [x]`.

- [x] **Step 5: Commit Phase 16 slice**

Run:

```bash
git add backend/src/test/java/com/example/aiticket/docs backend/src/main/resources/db/migration tools/smoke frontend/src docs/superpowers/plans/2026-06-20-phase-16-route-smoke-alignment-implementation-plan.md
git commit -m "fix: align demo routes and smoke script"
```
