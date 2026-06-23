# Phase 46 Ticket Detail Responsive Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the ticket detail page so workflow actions, SLA, comments, and flow logs remain clear and correctly ordered on narrow screens.

**Architecture:** Keep all ticket APIs and workflow behavior unchanged. Add stable panel class hooks in `TicketDetailView.vue`, then use CSS media queries to reorder panels and tighten the Apple-style card layout on mobile.

**Tech Stack:** Vue 3, Vite, TypeScript, CSS, Vitest, browser validation.

---

## Files

- Modify: `frontend/src/views/tickets/TicketDetailView.vue`
  - Add stable panel class hooks for hero, AI context, comments, SLA, actions, and flow log panels.
- Modify: `frontend/src/views/tickets/TicketDetailView.spec.ts`
  - Add a regression test proving the detail page exposes the required responsive panel hooks.
- Modify: `frontend/src/styles/main.css`
  - Keep desktop two-column detail layout.
  - On narrow screens, flatten main/side wrappers and order panels as:
    1. hero
    2. SLA
    3. status actions
    4. AI context
    5. comments
    6. flow logs
  - Improve action button and comment form spacing on narrow screens.
- Verify:
  - `npm run test -- TicketDetailView`
  - `npm run build`
  - Browser checks at desktop and 390px viewport.

---

## Tasks

### Task 1: Add Responsive Panel Hook Test

**Files:**
- Modify: `frontend/src/views/tickets/TicketDetailView.spec.ts`

- [x] ⭐ **Step 1: Add failing test**

Add a test that mounts `TicketDetailView` and asserts these hooks exist:

```ts
expect(wrapper.find('.ticket-hero-panel').exists()).toBe(true)
expect(wrapper.find('.ticket-context-panel').exists()).toBe(true)
expect(wrapper.find('.ticket-comments-panel').exists()).toBe(true)
expect(wrapper.find('.ticket-sla-panel').exists()).toBe(true)
expect(wrapper.find('.ticket-action-panel').exists()).toBe(true)
expect(wrapper.find('.ticket-flow-panel').exists()).toBe(true)
```

- [x] ⭐ **Step 2: Run test and confirm failure**

Run:

```bash
cd frontend
npm run test -- TicketDetailView
```

Expected: test fails because some new panel hooks are missing.

### Task 2: Add Ticket Detail Panel Hooks

**Files:**
- Modify: `frontend/src/views/tickets/TicketDetailView.vue`

- [x] ⭐ **Step 1: Add panel class hooks**

Add:

- `.ticket-hero-panel`
- `.ticket-context-panel`
- `.ticket-comments-panel`
- `.ticket-sla-panel`
- `.ticket-action-panel`
- `.ticket-flow-panel`

- [x] ⭐ **Step 2: Run test and confirm pass**

Run:

```bash
cd frontend
npm run test -- TicketDetailView
```

Expected: all TicketDetailView tests pass.

### Task 3: Add Mobile Detail Layout CSS

**Files:**
- Modify: `frontend/src/styles/main.css`

- [x] ⭐ **Step 1: Keep desktop two-column layout**

Above `820px`, keep:

```css
.ticket-detail-layout {
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
}
```

- [x] ⭐ **Step 2: Reorder panels on narrow screens**

Inside `@media (max-width: 820px)`, set:

- `.ticket-detail-main, .ticket-detail-side { display: contents; }`
- `.ticket-hero-panel { order: 1; }`
- `.ticket-sla-panel { order: 2; }`
- `.ticket-action-panel { order: 3; }`
- `.ticket-context-panel { order: 4; }`
- `.ticket-comments-panel { order: 5; }`
- `.ticket-flow-panel { order: 6; }`

- [x] ⭐ **Step 3: Improve mobile action controls**

On narrow screens:

- `.ticket-action-grid` should use one-column full-width buttons;
- `.comment-item div` should wrap cleanly;
- `.ticket-detail-meta` should keep chips readable;
- textareas and selects should not overflow.

### Task 4: Verify and Commit

**Files:**
- Verify and commit.

- [x] ⭐ **Step 1: Run tests**

Run:

```bash
cd frontend
npm run test -- TicketDetailView
```

- [x] ⭐ **Step 2: Run build**

Run:

```bash
cd frontend
npm run build
```

- [x] ⭐ **Step 3: Browser validation**

Use browser validation on a real ticket detail page, at desktop and 390px viewport.

Expected:

- desktop still has two-column layout;
- mobile uses one-column ordered panels;
- status actions appear before comments;
- no page-level horizontal overflow;
- buttons and textareas remain usable.

- [x] ⭐ **Step 4: Inspect diff and secrets**

Run:

```bash
git diff --check
git diff | rg -n "(?i)(api[_-]?key|secret|token|sk-|accessToken)" || true
```

- [ ] **Step 5: Commit and push**

Commit:

```bash
git add frontend/src/views/tickets/TicketDetailView.vue frontend/src/views/tickets/TicketDetailView.spec.ts frontend/src/styles/main.css docs/superpowers/plans/2026-06-23-phase-46-ticket-detail-responsive-polish-implementation-plan.md
git commit -m "style: improve responsive ticket detail layout"
```

Push to:

- `origin codex/recovered-phase31`
- `https://github.com/HelloWhatIsYourName/ticket-system.git HEAD:main`
