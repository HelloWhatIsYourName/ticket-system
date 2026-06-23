# Final Defense Demo Runbook

Date: 2026-06-23

This runbook is the final manual demo path for the AI knowledge-base ticket system after Phase 39/40. It covers startup, test accounts, RAG knowledge retrieval, AI-to-ticket transfer, assignment recommendation, SLA display, ticket processing, closure, and admin verification.

Use this document for live defense, screen recording rehearsal, and final manual acceptance. Keep secrets in `/private/tmp/ai-ticket-secrets`; do not copy API keys into the repository, screenshots, logs, README, thesis, or slides.

## 1. Current Scope

Implemented demo capabilities:

| Area | Status | Evidence |
| --- | --- | --- |
| RBAC login and route guard | Done | `admin`, `user`, `agent` role menus and 401/403 behavior |
| Knowledge-base ingestion and search | Done | `/app/knowledge` text document creation and retrieval test |
| RAG chat with citations and fallback | Done | `/app/ai/chat` SSE stream, citation cards, normal HTTP fallback |
| AI question to ticket | Done | `/api/tickets/from-ai-session`, `/app/tickets/my` |
| Manual ticket workflow | Done | assign, start, resolve, reopen, confirm close, manage close |
| Assignment recommendation | Done | admin ticket detail `智能推荐` block |
| SLA display | Done | list `SLA` column and detail `SLA 状态` panel |
| Admin statistics | Done | `/app/admin/dashboard` totals, processing, resolved, closed |
| System administration | Done | `/app/system` user, role, permission visibility |

Phase 39/40 intentionally does not implement automatic assignment, SLA background escalation jobs, priority edit recalculation, or notification delivery. The current result is a deterministic and demonstrable assignment recommendation plus priority-based SLA visibility.

## 2. Startup

Worktree:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31
```

Dependencies:

```bash
docker compose up -d
docker compose ps
```

Backend:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31/backend

set -a
source /private/tmp/ai-ticket-secrets/siliconflow.env
source /private/tmp/ai-ticket-secrets/deepseek.env
set +a

JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn spring-boot:run
```

Expected backend evidence:

```text
Tomcat started on port 8080
```

Backend reachability:

```bash
curl -i http://127.0.0.1:8080/api/auth/me
```

Expected:

```text
HTTP/1.1 401
```

Frontend:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31/frontend
npm run dev -- --host 127.0.0.1 --port 5175
```

Frontend reachability:

```bash
curl -I http://127.0.0.1:5175/
```

Expected:

```text
HTTP/1.1 200
```

Open:

```text
http://127.0.0.1:5175
```

## 3. Test Accounts

Seeded baseline accounts:

| Role | Username | Password | Demo Use |
| --- | --- | --- | --- |
| 管理员 | `admin` | `Admin_123456` | knowledge, assignment, dashboard, system admin |
| 普通用户 | `user` | `Admin_123456` | RAG question, create ticket, confirm close |
| 坐席 | `agent` | `Admin_123456` | assigned ticket processing |

Current recovered acceptance database may also contain:

| Role | Username | Password | Demo Use |
| --- | --- | --- | --- |
| 二线坐席 | `agent2` | `Admin_123456` | recommended assignee when its active workload is lower |

Important assignment note:

- The recommendation panel is authoritative. If it recommends `agent`, continue with the `agent` account.
- If it recommends `agent2`, either log in as `agent2 / Admin_123456` for the assigned-ticket step, or manually choose `agent` from the assignee dropdown if the defense script must use only the baseline accounts.
- This behavior is expected because Phase 39 ranks active AGENT users by current active workload.

## 4. Preflight Smoke

Run the acceptance evidence collector before a rehearsal:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31

FRONTEND_BASE_URL=http://127.0.0.1:5175 \
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
tools/smoke/phase31-acceptance-evidence.sh
```

Expected report checks:

```text
Backend smoke                  PASS
Frontend dev smoke             PASS
Frontend tests                 PASS
Frontend build                 PASS
Backend documentation coverage PASS
```

The script redacts tokens. If a log shows an actual token or API key, stop and remove the artifact before sharing any evidence.

## 5. Demo Path

### Step 1: Admin Creates Knowledge

Login:

```text
admin / Admin_123456
```

Open:

```text
/app/knowledge
```

Create a text document:

```text
Title: 密码重置 FAQ
Content: 用户忘记密码时，应在登录页选择忘记密码，完成身份验证后重置密码。如账号被锁定，可等待锁定时间结束或联系 IT 管理员解锁。
Category: 通用问题
```

Run retrieval test:

```text
忘记密码后应该如何重置？
```

Expected:

- document parses successfully;
- retrieval results include the password reset guidance;
- no raw embedding/vector value is shown.

### Step 2: User Asks AI and Creates Ticket

Logout, then login:

```text
user / Admin_123456
```

Open:

```text
/app/ai/chat
```

Ask:

```text
忘记密码后应该如何重置？
```

Expected:

- answer appears through stream rendering;
- citation cards are visible;
- if streaming fails, normal HTTP fallback still returns the answer.

Create a ticket from the AI answer. Use a question that needs manual support if the transfer form should be emphasized:

```text
我的账号被锁定，验证码也无法接收，需要人工确认。
```

Expected ticket fields:

```text
status = PENDING_ASSIGN
source = AI_SESSION
priority = selected priority
deadlineAt = present
slaStatus = ON_TRACK / DUE_SOON depending on current time and priority
```

Open:

```text
/app/tickets/my
```

Expected:

- new ticket appears;
- list includes the `SLA` column;
- the ticket detail can be opened.

### Step 3: Admin Uses Assignment Recommendation

Logout, then login:

```text
admin / Admin_123456
```

Open:

```text
/app/tickets/manage
```

Open the new `PENDING_ASSIGN` ticket detail.

Expected detail evidence:

- `SLA 状态` panel is visible;
- deadline is visible;
- remaining time is visible;
- `智能推荐` block is visible;
- recommendation reason explains the candidate and active workload;
- `使用推荐坐席` button is visible when a recommendation exists.

Click:

```text
使用推荐坐席
```

Expected:

- assignee dropdown changes to the recommended agent shown in the panel.

Fill action comment:

```text
使用智能推荐分配给坐席
```

Click:

```text
分配给坐席
```

Expected:

```text
PENDING_ASSIGN -> PENDING_PROCESS
```

Flow log should include:

```text
ASSIGN
使用智能推荐分配给坐席
```

Return to `/app/tickets/manage`.

Expected:

- ticket status is `待处理`;
- list still shows the `SLA` chip and deadline.

### Step 4: Agent Processes the Ticket

Logout, then login as the assigned agent.

If the ticket was assigned to baseline agent:

```text
agent / Admin_123456
```

If the recommendation chose second-line agent and that account exists:

```text
agent2 / Admin_123456
```

Open:

```text
/app/tickets/assigned
```

Expected:

- assigned ticket appears;
- list includes the `SLA` column;
- ticket detail shows `SLA 状态`.

Open the ticket detail, then click:

```text
开始处理
```

Use comment:

```text
开始排查账号锁定和验证码问题
```

Expected:

```text
PENDING_PROCESS -> PROCESSING
```

Add internal note:

```text
已核验账号状态，疑似触发安全锁定。
```

Click:

```text
标记解决
```

Use comment:

```text
已完成排查并给出解决方案，请用户确认。
```

Expected:

```text
PROCESSING -> RESOLVED
```

Flow log should show non-empty comments for `START_PROCESS` and `RESOLVE`.

### Step 5: User Confirms Close

Logout, then login:

```text
user / Admin_123456
```

Open:

```text
/app/tickets/my
```

Open the resolved ticket detail.

Click:

```text
确认关闭
```

Use comment:

```text
确认问题已解决
```

Expected:

```text
RESOLVED -> CLOSED
```

Flow log includes:

```text
CONFIRM_CLOSE
确认问题已解决
```

### Step 6: Admin Verifies Statistics and Permissions

Login:

```text
admin / Admin_123456
```

Open:

```text
/app/admin/dashboard
```

Expected:

- total tickets changes after the new ticket;
- processing/resolved/closed counts reflect workflow state;
- knowledge document and AI question metrics are visible.

Open:

```text
/app/system
```

Expected:

- users, roles, and permissions are visible to admin.

Verify ordinary user access:

1. Login as `user`.
2. Try opening `/app/system` or `/app/admin/dashboard`.
3. Expected result: access is blocked or redirected according to route guard and backend permissions.

## 6. What to Say During Defense

Short narration:

```text
The system first tries to answer from the knowledge base through vector retrieval and an OpenAI-compatible chat provider. If the user still needs manual support, the AI conversation can be converted into a ticket. Admin users keep manual control over assignment, but the system now recommends the lowest-load active agent and explains the reason. Ticket priority generates an SLA deadline at creation time, and both list and detail pages show SLA status. Agents process tickets through a controlled workflow, users confirm closure, and the admin dashboard reflects the final state.
```

Technical extension explanation:

```text
Assignment recommendation is read-only and non-invasive: it does not mutate ticket state until the admin clicks the existing assign action. SLA is deterministic: priority maps to a deadline, and the response DTO derives ON_TRACK, DUE_SOON, OVERDUE, or COMPLETED from current ticket status and deadline. These two features reuse the existing workflow service and response contracts, which demonstrates that the earlier architecture left enough extension space.
```

Known limits:

- The system recommends assignees but does not auto-assign.
- SLA does not trigger background escalation or notification jobs.
- Priority edits do not recalculate existing deadlines.
- Fresh seed data includes one baseline agent; recovered demo databases may include `agent2`.

## 7. Final Verification Commands

Backend:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn test
```

Frontend:

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31/frontend
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

Keep the generated report path in the rehearsal notes. Do not paste raw tokens or API keys into any document.
