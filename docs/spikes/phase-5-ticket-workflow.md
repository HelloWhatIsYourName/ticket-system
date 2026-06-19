# Phase 5 Ticket Workflow Spike

Date: 2026-06-20

## Scope

This verification covers the Phase 5 ticket workflow backend path:

- Oracle V5 migration for ticket categories, tickets, comments, and workflow logs
- ticket creation from a persisted Phase 4 AI session
- user-owned, agent-assigned, and admin-managed ticket lists
- manual assignment by an administrator
- agent processing and resolving transitions
- user reopen and confirm-close transitions
- RBAC denial for unauthorized and wrong-role calls

Frontend ticket pages, statistics dashboards, SLA escalation, and automatic assignee selection remain later scope.

## Environment

- Branch/worktree: `knowledge-live-verification`
- Backend: Spring Boot on `127.0.0.1:8080`
- Oracle container: `ai-ticket-oracle`, port `1521`
- Redis container: `ai-ticket-redis`, port `6379`
- Embedding provider: SiliconFlow `Qwen/Qwen3-Embedding-8B`
- Chat provider: local OpenAI-compatible mock on `127.0.0.1:18080`
- Secret handling: `AI_EMBEDDING_API_KEY` was loaded from `/private/tmp/ai-ticket-secrets/siliconflow.env`; keys and JWTs were not printed

The real live chat provider call was skipped because `AI_CHAT_API_KEY` was not available locally. Ticket workflow verification used a real persisted AI session created through the Phase 4 RAG API with mock chat and real embedding/vector retrieval.

## Unit Verification

Focused ticket tests:

```text
TicketMapperXmlTest, TicketWorkflowServiceTest, TicketControllerTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend suite:

```text
mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Live Verification

Flyway startup confirmed the Phase 5 schema:

```text
Successfully validated 5 migrations
Migrating schema "AI_TICKET" to version "5 - ticket workflow"
Successfully applied 1 migration to schema "AI_TICKET", now at version v5
```

Sanitized API verification output:

```json
[
  {
    "step": "admin_login",
    "status": 200,
    "hasAssign": true,
    "hasManage": true
  },
  {
    "step": "user_login",
    "status": 200,
    "hasCreate": true,
    "hasOwnView": true
  },
  {
    "step": "agent_login",
    "status": 200,
    "hasProcess": true
  },
  {
    "step": "create_kb_text",
    "status": 200,
    "documentId": 11,
    "parseStatus": "PARSE_SUCCESS"
  },
  {
    "step": "ai_ask",
    "status": 200,
    "sessionId": 9,
    "assistantMessageId": 18,
    "transferSuggested": true,
    "citationCount": 5
  },
  {
    "step": "create_ticket",
    "status": 200,
    "ticketId": 1,
    "ticketNoPresent": true,
    "ticketStatus": "PENDING_ASSIGN",
    "sourceSessionId": 9
  },
  {
    "step": "my_tickets",
    "status": 200,
    "count": 1,
    "containsCreated": true
  },
  {
    "step": "manage_tickets",
    "status": 200,
    "count": 1
  },
  {
    "step": "assign",
    "status": 200,
    "ticketStatus": "PENDING_PROCESS",
    "assigneeId": 3
  },
  {
    "step": "assigned_tickets",
    "status": 200,
    "count": 1,
    "containsCreated": true
  },
  {
    "step": "start",
    "status": 200,
    "ticketStatus": "PROCESSING"
  },
  {
    "step": "resolve_first",
    "status": 200,
    "ticketStatus": "RESOLVED",
    "firstResolvedPresent": true
  },
  {
    "step": "reopen",
    "status": 200,
    "ticketStatus": "PROCESSING",
    "reopenCount": 1
  },
  {
    "step": "resolve_second",
    "status": 200,
    "ticketStatus": "RESOLVED"
  },
  {
    "step": "confirm_close",
    "status": 200,
    "ticketStatus": "CLOSED",
    "closedPresent": true
  },
  {
    "step": "user_assign_forbidden",
    "status": 403
  },
  {
    "step": "anonymous_my_unauthorized",
    "status": 401
  }
]
```

## Notes

Workflow log creation is covered by `TicketWorkflowServiceTest`, which asserts that successful create, assign, start, resolve, reopen, confirm-close, and close operations insert the expected `ticket_flow_log` actions. The first REST API does not expose flow logs yet; a later management/detail API can add read-only flow-log history without changing the write-side workflow service.

## Result

Phase 5 ticket workflow backend is verified for creating tickets from AI sessions, manual admin assignment, agent processing, user reopen/closure, scoped lists, and RBAC. The implementation keeps workflow mutation centralized in `TicketWorkflowService`, leaving room for SLA hooks, automatic assignment strategies, flow-log history APIs, and statistics in later tasks.
