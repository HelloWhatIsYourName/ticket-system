# V1 Demo Runbook

This runbook prepares a 10-15 minute defense demo for the AI knowledge-base ticket system. It assumes the backend, frontend, Oracle 23ai, Redis, and AI provider or mock provider are available.

## 1. Environment Startup

Start dependencies and backend:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn spring-boot:run
```

Expected startup evidence:

```text
Tomcat started on port 8080
Successfully validated 5 migrations
Schema "AI_TICKET" is up to date
```

Use the repeatable smoke script when a full backend check is needed:

```bash
BASE_URL=http://127.0.0.1:8080 tools/smoke/phase7-backend-smoke.sh
```

The script prints `token:redacted` and must not expose JWT values.

Start the frontend in another terminal:

```bash
cd frontend
npm run dev
```

Open the printed Vite URL, then use the app route:

```text
/app/demo
```

The `/app/demo` page is the frontend checklist for the defense path and links to every implemented product screen.

## 2. Login and RBAC

Demo accounts:

| Role | Username | Purpose |
| --- | --- | --- |
| 管理员 | `admin` | 知识库, 工单管理, 统计, 用户角色权限 |
| 普通用户 | `user` | RAG 问答, 转工单, 查看本人工单 |
| 坐席 | `agent` | 查看分配工单, 开始处理, 解决工单 |

Show:

1. `admin` login succeeds.
2. `user` login succeeds.
3. Direct access to `/app/...` without a token redirects to `/login?redirect=...`.
4. Refreshing `/app/...` restores the current user through `/api/auth/me`.
5. Anonymous admin request returns `401`.
6. `user` calling admin users or 统计 endpoint returns `403`.

This demonstrates RBAC and method-level permission enforcement.

## 3. Knowledge Base Preparation

As `admin`, create a text knowledge document for a stable demo question:

```text
Title: 密码重置 FAQ
Content: 用户忘记密码时，应在登录页选择忘记密码，完成身份验证后重置密码。如账号被锁定，可等待锁定时间结束或联系 IT 管理员解锁。
Category: 通用问题
```

Expected result:

```text
parseStatus = PARSED
```

In the frontend, open `/app/knowledge`, create the text document, then run 检索测试 with:

```text
忘记密码后应该如何重置？
```

Expected result: at least one chunk contains password reset guidance.

## 4. RAG Question

Open `/app/ai/chat` as `user` and ask:

```text
忘记密码后应该如何重置？
```

Show:

1. RAG returns an answer grounded in the knowledge document.
2. Response includes 引用来源 cards.
3. The answer does not expose raw vector fields.

For thesis evaluation, compare this question against `docs/evaluation/rag-evaluation-set.json` and record 检索命中, 回答有用率, and whether transfer behavior is correct.

## 5. Manual Transfer to Ticket

Use the right-side `转为工单` form on `/app/ai/chat` when the user still needs manual confirmation.

Expected fields:

```text
status = PENDING_ASSIGN
sourceSessionId = AI session id
ticketNo is present
```

Show that the user can see the ticket in `/app/tickets/my` and open the detail page.

## 6. Admin Assignment

As `admin`, assign the ticket to `agent`.

Expected transition:

```text
PENDING_ASSIGN -> PENDING_PROCESS
```

Show that the admin can also open `/app/admin/dashboard`:

```text
totalTickets
pendingTickets
processingTickets
resolvedTickets
knowledgeHitRate
```

This connects 工单 operations with management 统计.

## 7. Agent Processing

As `agent`:

1. Open `/app/tickets/my` or the linked ticket detail if assigned.
2. Start processing from the ticket detail page.
3. Add an internal note if needed.
4. Resolve the ticket from the ticket detail page.

Expected transitions:

```text
PENDING_PROCESS -> PROCESSING -> RESOLVED
```

The flow log should record each state change.

## 8. User Feedback and Closure

As `user`:

1. Add a public reply if more information is needed.
2. Confirm close when resolved.

Expected transition:

```text
RESOLVED -> CLOSED
```

This demonstrates the user-side feedback loop currently implemented through `ticket_comment` and ticket workflow actions.

## 9. Admin Management Screens

Show these implemented frontend screens:

| Area | Frontend Route | Demo Point |
| --- | --- | --- |
| 演示导览 | `/app/demo` | clickable defense path |
| 知识库 | `/app/knowledge` | text ingestion and retrieval test |
| AI 问答 | `/app/ai/chat` | answer, citations, transfer form |
| 工单 | `/app/tickets/my`, `/app/tickets/:id` | list, detail, replies, notes, workflow actions |
| 统计 | `/app/admin/dashboard` | overview, category stats, hot questions |
| 用户角色权限 | `/app/system` | list, enable/disable, role assignment, permission visibility |
| Redis | runtime dependency | document parsing queue and future hot ranking |

## 10. Closing Narrative

End the demo with this summary:

```text
The system implements the first-version end-to-end loop: RBAC login with session restore, 知识库 ingestion and retrieval, RAG answer with citations, transfer to 工单, ticket comments and workflow actions, admin 统计, and system role management. The frontend now provides the demonstration path, while backend smoke scripts and the RAG evaluation set remain the repeatable verification evidence.
```
