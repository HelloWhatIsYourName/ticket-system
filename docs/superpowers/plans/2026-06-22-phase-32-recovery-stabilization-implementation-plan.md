# Phase 32 Recovery Stabilization Implementation Plan

## Goal

Recover from the disrupted local environment by using the clean Phase 31 GitHub baseline, then restore only the known acceptance-blocking fixes needed for reliable manual testing.

## Tasks

- [x] ⭐ Create a clean recovered worktree from `origin/main` at `a4540d7`.
- [x] ⭐ Verify the recovered Phase 31 baseline with backend tests, frontend tests, and frontend build.
- [x] ⭐ Fix login failure handling so invalid credentials return a controlled 401 response instead of a 500.
- [x] ⭐ Verify the recovered backend and frontend can run from this worktree without using the older messy checkout.
- [x] ⭐ Restore the admin ticket management route so `/app/tickets/manage` opens the ticket list instead of a `NaN` ticket detail.
- [x] ⭐ Audit the known manual-test gaps and select the next smallest restoration slice.
- [x] ⭐ Fix ticket detail workflow actions so only valid actions are shown and successful status changes refresh flow logs with the submitted remark.
- [x] ⭐ Add an admin assignment control on ticket details so pending tickets can be assigned to an active agent without leaving the manual-test flow.
- [x] ⭐ Align ticket flow-log rendering with the backend `commentText` field so submitted workflow remarks are displayed instead of `无备注`.
- [x] ⭐ Add frontend route permission checks so regular users are redirected away from administrator-only pages before protected admin APIs are called.
- [x] ⭐ Add frontend coverage for creator close/reopen actions on resolved tickets so the manual workflow closure is protected by tests.
- [x] ⭐ Restore admin dashboard status counters for processing, resolved, and closed tickets so manual statistics checks match backend data.

## Verification

- `backend`: `mvn test`
- `frontend`: `npm run test`
- `frontend`: `npm run build`
- Runtime checks:
  - `GET /api/auth/me` returns 401 before login.
  - Invalid login returns 401 with an API failure body.
  - Valid login returns 200 with user, roles, permissions, and menus.
