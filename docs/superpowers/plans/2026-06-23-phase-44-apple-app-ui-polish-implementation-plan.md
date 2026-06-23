# Phase 44 Apple App UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the authenticated application UI and login page with an Apple-inspired light product interface while keeping the existing home page unchanged.

**Architecture:** This phase is a visual polish pass. It keeps Vue routes, API calls, permissions, stores, and backend behavior unchanged. Most changes are centralized in `frontend/src/styles/main.css`, with a small `AppShell.vue` markup refinement to support better Apple-style navigation and workspace chrome.

**Tech Stack:** Vue 3, Vite, TypeScript, Element Plus, CSS, Vitest, browser smoke validation.

---

## Files

- Modify: `frontend/src/styles/main.css`
  - Add Apple app design tokens.
  - Restyle login, application shell, panels, forms, tables, chips, dashboard, tickets, knowledge, chat, system, and demo views.
  - Do not modify `.home-page`, `.home-nav`, `component--*`, `.display`, `.text`, or home-specific selectors.
- Modify: `frontend/src/layouts/AppShell.vue`
  - Add small structural hooks for sidebar header, active app menu, and user chip.
  - Keep route generation and logout behavior unchanged.
- Test: `frontend/src/layouts/AppShell.spec.ts`
  - Existing text assertions must continue to pass.
- Verify:
  - `npm run test`
  - `npm run build`
  - Browser check for `/`, `/login`, and authenticated `/app/*` pages.

---

## Tasks

### Task 1: Add Apple App Tokens and Plan Guardrails

**Files:**
- Modify: `frontend/src/styles/main.css`

- [x] ⭐ **Step 1: Add application-only Apple design tokens**

Add tokens to `:root` without changing the existing `--home-*` variables used by the home page:

```css
:root {
  --app-bg: #f5f5f7;
  --app-surface: #ffffff;
  --app-surface-soft: #fbfbfd;
  --app-surface-elevated: rgb(255 255 255 / 0.82);
  --app-text: #1d1d1f;
  --app-muted: #6e6e73;
  --app-muted-strong: #424245;
  --app-blue: #0066cc;
  --app-blue-hover: #0071e3;
  --app-blue-soft: #e8f2ff;
  --app-border: #d2d2d7;
  --app-border-soft: #e8e8ed;
  --app-danger: #b42318;
  --app-danger-soft: #fff1f0;
  --app-warning: #8a5a00;
  --app-warning-soft: #fff7e6;
  --app-success: #137333;
  --app-success-soft: #eaf7ee;
  --app-radius-card: 18px;
  --app-radius-control: 12px;
  --app-radius-pill: 999px;
}
```

- [x] ⭐ **Step 2: Keep home page selectors untouched**

Before editing, confirm the home page selectors remain in place:

```bash
rg -n "home-page|home-nav|component--herosolutions|HomeView" frontend/src/styles/main.css frontend/src/views/HomeView.vue frontend/src/components/home
```

Expected: command finds home page styling and components. This phase must not remove or restyle those selectors.

### Task 2: Restyle Login and Authenticated App Shell

**Files:**
- Modify: `frontend/src/layouts/AppShell.vue`
- Modify: `frontend/src/styles/main.css`
- Test: `frontend/src/layouts/AppShell.spec.ts`

- [x] ⭐ **Step 1: Add shell structure hooks**

Update `AppShell.vue` to wrap the brand and user controls with explicit class hooks:

```vue
<aside class="app-sidebar" aria-label="Application navigation">
  <div class="app-sidebar-head">
    <RouterLink class="app-shell-brand" to="/">AI Knowledge Ticket</RouterLink>
    <span>Service Desk</span>
  </div>
  <nav class="app-shell-menu">
    <RouterLink v-for="menu in menus" :key="menu.code || menu.path" :to="menu.path">
      {{ menu.name }}
    </RouterLink>
  </nav>
</aside>
```

```vue
<div class="app-user">
  <span class="app-user-avatar">{{ displayName.slice(0, 1).toUpperCase() }}</span>
  <span>{{ displayName }}</span>
  <button type="button" @click="logout">退出</button>
</div>
```

- [x] ⭐ **Step 2: Restyle login and shell**

Use `--app-*` tokens for `.login-view`, `.login-panel`, `.app-shell`, `.app-sidebar`, `.app-topbar`, `.app-content`, `.app-user`, and `.placeholder-view`.

- [x] ⭐ **Step 3: Run AppShell test**

Run:

```bash
cd frontend
npm run test -- AppShell
```

Expected: AppShell spec passes and still finds menu labels plus display name.

### Task 3: Restyle Shared Workspace Components

**Files:**
- Modify: `frontend/src/styles/main.css`

- [x] ⭐ **Step 1: Restyle shared page headers, state panels, forms, and buttons**

Apply Apple tokens to:

```text
.workspace-page-header
.state-panel
.state-skeleton
.panel-heading
form labels, inputs, textareas, selects, and submit buttons used by chat, knowledge, tickets, and system pages
```

- [x] ⭐ **Step 2: Keep interaction states clear**

Every primary button must use Action Blue, pill radius, visible disabled state, focus ring, and a small active press transform.

### Task 4: Restyle Business Pages

**Files:**
- Modify: `frontend/src/styles/main.css`

- [x] ⭐ **Step 1: Restyle dashboard and demo cards**

Apply Apple utility-card style to dashboard stats, dashboard panels, dashboard lists, and demo steps.

- [x] ⭐ **Step 2: Restyle RAG chat**

Apply Apple surface, border, input, answer, citation, session, and transfer panel styles without changing chat behavior.

- [x] ⭐ **Step 3: Restyle ticket list and detail**

Apply Apple surface, row, chip, detail panel, comment, flow log, SLA, and recommendation panel styles.

- [x] ⭐ **Step 4: Restyle knowledge and system pages**

Apply Apple surface, document cards, result cards, user rows, role rows, permission modules, and selected states.

### Task 5: Verify Frontend and Browser Acceptance

**Files:**
- Verify only.

- [x] ⭐ **Step 1: Run frontend test suite**

Run:

```bash
cd frontend
npm run test
```

Expected: all Vitest specs pass.

- [x] ⭐ **Step 2: Run production build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build pass.

- [x] ⭐ **Step 3: Browser check**

Start the frontend if needed:

```bash
cd frontend
npm run dev -- --host 127.0.0.1 --port 5175
```

Check:

```text
http://127.0.0.1:5175/
http://127.0.0.1:5175/login
http://127.0.0.1:5175/app/demo
http://127.0.0.1:5175/app/knowledge
http://127.0.0.1:5175/app/ai/chat
http://127.0.0.1:5175/app/tickets/my
http://127.0.0.1:5175/app/admin/dashboard
http://127.0.0.1:5175/app/system
```

Expected:

- `/` home page keeps its existing design.
- `/login` uses the Apple app style.
- `/app/*` pages use the new Apple app shell and business-page styling.
- No obvious overlap, broken text, invisible buttons, or unusable controls at desktop and mobile widths.

### Task 6: Commit and Push

**Files:**
- Modify: `frontend/src/styles/main.css`
- Modify: `frontend/src/layouts/AppShell.vue`
- Modify: `docs/superpowers/plans/2026-06-23-phase-44-apple-app-ui-polish-implementation-plan.md`

- [x] ⭐ **Step 1: Inspect diff**

Run:

```bash
git diff --stat
git diff --check
git diff | rg -n "(?i)(api[_-]?key|secret|token|sk-|accessToken)" || true
```

Expected: only visual/code/docs changes, no whitespace errors, no real secrets.

- [x] ⭐ **Step 2: Commit**

Run:

```bash
git add frontend/src/styles/main.css frontend/src/layouts/AppShell.vue docs/superpowers/plans/2026-06-23-phase-44-apple-app-ui-polish-implementation-plan.md
git commit -m "style: polish app ui with apple inspired design"
```

- [x] ⭐ **Step 3: Push**

Push to the active branch first. If the user wants release/main updated, push the resulting commit to `HelloWhatIsYourName/ticket-system.git main`.
