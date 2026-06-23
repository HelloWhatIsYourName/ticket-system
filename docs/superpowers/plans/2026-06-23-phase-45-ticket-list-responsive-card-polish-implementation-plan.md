# Phase 45 Ticket List Responsive Card Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make ticket lists read as compact Apple-style cards on narrow screens while keeping the desktop table layout.

**Architecture:** Keep ticket APIs, routing, permissions, and data loading unchanged. Add semantic cell classes and labels in `TicketListView.vue`, then use CSS media queries to switch the same markup from desktop grid rows to mobile cards.

**Tech Stack:** Vue 3, Vite, TypeScript, CSS, Vitest, browser validation.

---

## Files

- Modify: `frontend/src/views/tickets/TicketListView.vue`
  - Add class hooks and `data-label` attributes for responsive card layout.
- Modify: `frontend/src/views/tickets/TicketListView.spec.ts`
  - Add a regression test proving rendered ticket cells expose mobile labels.
- Modify: `frontend/src/styles/main.css`
  - Keep desktop table layout.
  - Convert `.ticket-row` entries to card-like rows below `820px`.
  - Hide the table header on narrow screens.
- Verify:
  - `npm run test -- TicketListView`
  - `npm run build`
  - Browser checks at desktop and 390px viewport.

---

## Tasks

### Task 1: Add Responsive Markup Test

**Files:**
- Modify: `frontend/src/views/tickets/TicketListView.spec.ts`

- [x] ⭐ **Step 1: Add failing test**

Add a test that mounts a ticket with SLA and verifies responsive labels exist:

```ts
expect(wrapper.find('[data-label="状态"]').exists()).toBe(true)
expect(wrapper.find('[data-label="优先级"]').exists()).toBe(true)
expect(wrapper.find('[data-label="SLA"]').exists()).toBe(true)
expect(wrapper.find('[data-label="来源"]').exists()).toBe(true)
expect(wrapper.find('[data-label="创建时间"]').exists()).toBe(true)
```

- [x] ⭐ **Step 2: Run test and confirm failure**

Run:

```bash
cd frontend
npm run test -- TicketListView
```

Expected: new test fails because `data-label` hooks do not exist yet.

### Task 2: Add Responsive Cell Hooks

**Files:**
- Modify: `frontend/src/views/tickets/TicketListView.vue`

- [x] ⭐ **Step 1: Add table/card class hooks**

Add classes:

- `.ticket-cell-no`
- `.ticket-cell-title`
- `.ticket-cell-status`
- `.ticket-cell-priority`
- `.ticket-cell-sla`
- `.ticket-cell-source`
- `.ticket-cell-created`

- [x] ⭐ **Step 2: Add `data-label` attributes**

Add `data-label` to non-primary cells so CSS can show labels on card layout:

- `data-label="状态"`
- `data-label="优先级"`
- `data-label="SLA"`
- `data-label="来源"`
- `data-label="创建时间"`

- [x] ⭐ **Step 3: Run test and confirm pass**

Run:

```bash
cd frontend
npm run test -- TicketListView
```

Expected: all TicketListView tests pass.

### Task 3: Add Mobile Card CSS

**Files:**
- Modify: `frontend/src/styles/main.css`

- [x] ⭐ **Step 1: Keep desktop table behavior**

Ensure existing desktop table layout remains active above `820px`.

- [x] ⭐ **Step 2: Add narrow-screen card layout**

Inside `@media (max-width: 820px)`, make:

- `.ticket-table` use vertical card spacing;
- `.ticket-row-head` hidden;
- each non-head `.ticket-row` become a single card;
- status and SLA chips wrap cleanly;
- secondary fields show `data-label` labels.

- [x] ⭐ **Step 3: Ensure no horizontal page overflow**

At 390px width, `.ticket-table` should not force page-level horizontal overflow.

### Task 4: Verify and Commit

**Files:**
- Verify and commit.

- [x] ⭐ **Step 1: Run tests**

Run:

```bash
cd frontend
npm run test -- TicketListView
```

- [x] ⭐ **Step 2: Run build**

Run:

```bash
cd frontend
npm run build
```

- [x] ⭐ **Step 3: Browser validation**

Use browser validation on:

```text
http://127.0.0.1:5175/app/tickets/manage
http://127.0.0.1:5175/app/tickets/my
http://127.0.0.1:5175/app/tickets/assigned
```

Expected:

- desktop width remains table-like;
- 390px width uses card-like rows;
- no visible overlap;
- no page-level horizontal overflow.

- [x] ⭐ **Step 4: Inspect diff and secrets**

Run:

```bash
git diff --check
git diff | rg -n "(?i)(api[_-]?key|secret|token|sk-|accessToken)" || true
```

- [x] ⭐ **Step 5: Commit and push**

Commit:

```bash
git add frontend/src/views/tickets/TicketListView.vue frontend/src/views/tickets/TicketListView.spec.ts frontend/src/styles/main.css docs/superpowers/plans/2026-06-23-phase-45-ticket-list-responsive-card-polish-implementation-plan.md
git commit -m "style: improve responsive ticket list cards"
```

Push to:

- `origin codex/recovered-phase31`
- `https://github.com/HelloWhatIsYourName/ticket-system.git HEAD:main`
