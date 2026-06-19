# Phase 5 Ticket Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Implement the backend ticket workflow foundation that creates tickets from AI sessions and records auditable status transitions.

**Architecture:** Add a focused `ticket` module. Controllers expose RBAC-protected APIs, `TicketWorkflowService` owns creation and transition rules, and MyBatis XML owns Oracle persistence. Phase 5 consumes persisted Phase 4 AI sessions/messages instead of re-running RAG.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security method authorization, MyBatis XML, Flyway Oracle migrations, JUnit 5.

---

## File Structure

```text
backend/
  src/main/resources/db/migration/V5__ticket_workflow.sql
  src/main/java/com/example/aiticket/ticket/domain/Ticket.java
  src/main/java/com/example/aiticket/ticket/domain/TicketCategory.java
  src/main/java/com/example/aiticket/ticket/domain/TicketFlowLog.java
  src/main/java/com/example/aiticket/ticket/domain/TicketPriority.java
  src/main/java/com/example/aiticket/ticket/domain/TicketSource.java
  src/main/java/com/example/aiticket/ticket/domain/TicketStatus.java
  src/main/java/com/example/aiticket/ticket/domain/TicketWorkflowAction.java
  src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java
  src/main/resources/mapper/TicketMapper.xml
  src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java
  src/main/java/com/example/aiticket/ticket/service/TicketNotFoundException.java
  src/main/java/com/example/aiticket/ticket/service/TicketWorkflowException.java
  src/main/java/com/example/aiticket/ticket/web/*.java
  src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java
  src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java
  src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java
docs/spikes/phase-5-ticket-workflow.md
```

## Task 1: Schema, Domain, Mapper, and Service Foundation ⭐

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__ticket_workflow.sql`
- Create: `backend/src/main/java/com/example/aiticket/ticket/domain/*.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java`
- Create: `backend/src/main/resources/mapper/TicketMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/*.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java`

- [x] **Step 1: Write mapper XML and migration tests**

Create `TicketMapperXmlTest` that reads `V5__ticket_workflow.sql` and `TicketMapper.xml`. Assert the migration defines `ticket`, `ticket_flow_log`, `ticket_category`, `ticket_comment`, status checks, and source AI session foreign keys. Assert mapper XML declares `insertTicket`, `insertFlowLog`, `findOwnedAiSession`, `findLatestAssistantMessage`, and `findTicketForUpdate`.

- [x] **Step 2: Run mapper test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest test
```

Expected: fail because V5 migration and mapper do not exist.

- [x] **Step 3: Add migration, domain records, mapper interface, and XML**

Implement V5 tables and sequences. Keep workflow enums as Java enums and Oracle `VARCHAR2` values. Use explicit JDBC types for nullable numeric and text fields.

- [x] **Step 4: Run mapper test to verify GREEN**

Run the same `TicketMapperXmlTest` command. Expected: tests pass.

- [x] **Step 5: Write service tests**

Create `TicketWorkflowServiceTest` with fake `TicketMapper`. Cover:

1. creating a ticket from an owned AI session inserts ticket and `CREATE` flow log;
2. non-owned AI session throws `TicketWorkflowException`;
3. assigning a pending ticket moves it to `PENDING_PROCESS`;
4. invalid transition throws and does not update status.

- [x] **Step 6: Run service test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketWorkflowServiceTest test
```

Expected: fail because service types do not exist yet.

- [x] **Step 7: Implement `TicketWorkflowService`**

Implement create-from-AI-session and manual assignment. Generate ticket numbers with `TK` + timestamp + ticket ID. Centralize allowed transitions and log every successful mutation.

- [x] **Step 8: Run focused Task 1 tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest,TicketWorkflowServiceTest test
```

- [x] **Step 9: Commit Task 1**

```bash
git add backend/src/main/resources/db/migration/V5__ticket_workflow.sql backend/src/main/java/com/example/aiticket/ticket backend/src/main/resources/mapper/TicketMapper.xml backend/src/test/java/com/example/aiticket/ticket docs/superpowers/plans/2026-06-20-phase-5-ticket-workflow-implementation-plan.md
git commit -m "feat: add ticket workflow foundation"
```

## Task 2: Ticket REST API ⭐

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/*.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java`

- [x] **Step 1: Write controller tests**

Cover permission annotations for create/list/detail/assign/start/resolve/reopen/confirm-close/close and response mapping with no internal workflow-only fields.

- [x] **Step 2: Run controller test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketControllerTest test
```

- [x] **Step 3: Implement DTOs and controller**

Expose:

```text
POST /api/tickets/from-ai-session
GET  /api/tickets/my
GET  /api/tickets/assigned
GET  /api/tickets/manage
GET  /api/tickets/{ticketId}
POST /api/tickets/{ticketId}/assign
POST /api/tickets/{ticketId}/start
POST /api/tickets/{ticketId}/resolve
POST /api/tickets/{ticketId}/reopen
POST /api/tickets/{ticketId}/confirm-close
POST /api/tickets/{ticketId}/close
```

- [x] **Step 4: Run focused controller tests**

Run the same `TicketControllerTest` command. Expected: tests pass.

- [x] **Step 5: Commit Task 2**

```bash
git add backend/src/main/java/com/example/aiticket/ticket/web backend/src/test/java/com/example/aiticket/ticket/web docs/superpowers/plans/2026-06-20-phase-5-ticket-workflow-implementation-plan.md
git commit -m "feat: expose ticket workflow api"
```

## Task 3: Full Verification and Live Spike ⭐

**Files:**
- Create: `docs/spikes/phase-5-ticket-workflow.md`
- Modify: `docs/superpowers/plans/2026-06-20-phase-5-ticket-workflow-implementation-plan.md`

- [x] **Step 1: Run all backend tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
```

- [x] **Step 2: Run local backend and verify ticket creation**

Use existing admin/user/agent accounts. Create or reuse an AI session, create a ticket from it as `user`, verify `my` list contains it, assign as `admin`, and verify `agent` sees it in assigned tickets.

- [x] **Step 3: Verify workflow transitions and RBAC**

Verify invalid anonymous calls return `401`, wrong-role operations return `403`, and valid status transitions create flow logs.

- [x] **Step 4: Record spike report**

Write sanitized evidence to `docs/spikes/phase-5-ticket-workflow.md`.

- [x] **Step 5: Final diff and plan marking**

Run `git diff --check`, inspect scope, mark completed tasks with checkboxes and stars.

- [x] **Step 6: Commit Task 3**

```bash
git add docs/spikes/phase-5-ticket-workflow.md docs/superpowers/plans/2026-06-20-phase-5-ticket-workflow-implementation-plan.md
git commit -m "docs: verify phase 5 ticket workflow"
```

## Task 4: Ticket Flow Log Detail Read Model ⭐

**Files:**
- Modify: `backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java`
- Modify: `backend/src/main/resources/mapper/TicketMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/ticket/domain/TicketDetail.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/web/TicketController.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/TicketDetailResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/TicketFlowLogResponse.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java`

- [x] **Step 1: Write failing tests**

Assert mapper XML declares `listFlowLogs`. Assert service `getTicketDetail(...)` returns a visible ticket plus ordered flow logs. Assert controller detail response includes `flowLogs` while list responses stay compact.

- [x] **Step 2: Run focused tests to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest,TicketWorkflowServiceTest,TicketControllerTest test
```

- [x] **Step 3: Implement flow-log read model**

Add `TicketDetail`, `TicketDetailResponse`, and `TicketFlowLogResponse`. Keep writes centralized in `TicketWorkflowService`; flow-log read access must reuse the same visibility checks as ticket detail.

- [x] **Step 4: Run focused and full tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest,TicketWorkflowServiceTest,TicketControllerTest test
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
```

- [x] **Step 5: Commit Task 4**

```bash
git add backend/src/main/java/com/example/aiticket/ticket backend/src/main/resources/mapper/TicketMapper.xml backend/src/test/java/com/example/aiticket/ticket docs/superpowers/plans/2026-06-20-phase-5-ticket-workflow-implementation-plan.md
git commit -m "feat: expose ticket flow log detail"
```

## Current Execution Note

Task 1, Task 2, and Task 3 are complete. Task 4 adds the read-side flow-log detail needed by frontend ticket detail pages.
