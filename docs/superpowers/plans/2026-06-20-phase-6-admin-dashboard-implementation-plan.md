# Phase 6 Admin Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Build the Phase 6 backend management and statistics APIs that support the admin dashboard, ticket category management, and basic user/role administration.

**Architecture:** Keep Phase 6 as a backend-first slice because this repository currently has no frontend project. Add focused `admin` and `system.web` APIs, keep SQL aggregation in MyBatis XML, and keep controllers thin with `@PreAuthorize` checks. The future Vue3 + Element Plus + ECharts frontend can consume these APIs without changing backend contracts.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security method authorization, MyBatis XML, Oracle SQL aggregation, JUnit 5.

---

## File Structure

```text
backend/
  src/main/java/com/example/aiticket/admin/statistics/domain/AdminDashboardOverview.java
  src/main/java/com/example/aiticket/admin/statistics/domain/TicketCategoryStat.java
  src/main/java/com/example/aiticket/admin/statistics/domain/HotQuestionStat.java
  src/main/java/com/example/aiticket/admin/statistics/mapper/AdminStatisticsMapper.java
  src/main/resources/mapper/AdminStatisticsMapper.xml
  src/main/java/com/example/aiticket/admin/statistics/service/AdminStatisticsService.java
  src/main/java/com/example/aiticket/admin/statistics/web/AdminStatisticsController.java
  src/main/java/com/example/aiticket/admin/statistics/web/*.java
  src/main/java/com/example/aiticket/ticket/service/TicketCategoryService.java
  src/main/java/com/example/aiticket/ticket/web/TicketCategoryController.java
  src/main/java/com/example/aiticket/ticket/web/TicketCategoryResponse.java
  src/main/java/com/example/aiticket/ticket/web/CreateTicketCategoryRequest.java
  src/main/java/com/example/aiticket/ticket/web/UpdateTicketCategoryRequest.java
  src/main/java/com/example/aiticket/system/SystemAdminMapper.java
  src/main/resources/mapper/SystemAdminMapper.xml
  src/main/java/com/example/aiticket/system/SystemAdminService.java
  src/main/java/com/example/aiticket/system/web/SystemAdminController.java
  src/main/java/com/example/aiticket/system/web/*.java
  src/test/java/com/example/aiticket/admin/statistics/mapper/AdminStatisticsMapperXmlTest.java
  src/test/java/com/example/aiticket/admin/statistics/service/AdminStatisticsServiceTest.java
  src/test/java/com/example/aiticket/admin/statistics/web/AdminStatisticsControllerTest.java
  src/test/java/com/example/aiticket/ticket/service/TicketCategoryServiceTest.java
  src/test/java/com/example/aiticket/ticket/web/TicketCategoryControllerTest.java
  src/test/java/com/example/aiticket/system/SystemAdminMapperXmlTest.java
  src/test/java/com/example/aiticket/system/SystemAdminServiceTest.java
  src/test/java/com/example/aiticket/system/web/SystemAdminControllerTest.java
docs/spikes/phase-6-admin-dashboard.md
```

## Task 1: Admin Statistics Read Model ⭐

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/domain/AdminDashboardOverview.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/domain/TicketCategoryStat.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/domain/HotQuestionStat.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/mapper/AdminStatisticsMapper.java`
- Create: `backend/src/main/resources/mapper/AdminStatisticsMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/service/AdminStatisticsService.java`
- Test: `backend/src/test/java/com/example/aiticket/admin/statistics/mapper/AdminStatisticsMapperXmlTest.java`
- Test: `backend/src/test/java/com/example/aiticket/admin/statistics/service/AdminStatisticsServiceTest.java`

- [x] **Step 1: Write mapper XML tests**

Create `AdminStatisticsMapperXmlTest` that reads `AdminStatisticsMapper.xml` and asserts it declares:

```text
selectOverview
selectTicketCategoryStats
selectHotQuestions
COUNT(*)
AVG(
FROM ticket
FROM ai_message
```

- [x] **Step 2: Run mapper test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=AdminStatisticsMapperXmlTest test
```

Expected: fail because `AdminStatisticsMapper.xml` does not exist.

- [x] **Step 3: Add domain records and mapper interface**

Create records:

```java
public record AdminDashboardOverview(
        long totalTickets,
        long pendingTickets,
        long processingTickets,
        long resolvedTickets,
        long closedTickets,
        double averageResolveHours,
        long knowledgeDocuments,
        long aiQuestions,
        double knowledgeHitRate
) {
}

public record TicketCategoryStat(Long categoryId, String categoryName, long ticketCount) {
}

public record HotQuestionStat(String question, long askCount) {
}
```

Create mapper methods:

```java
AdminDashboardOverview selectOverview();

List<TicketCategoryStat> selectTicketCategoryStats(@Param("limit") int limit);

List<HotQuestionStat> selectHotQuestions(@Param("limit") int limit);
```

- [x] **Step 4: Add Oracle aggregation XML**

Implement `AdminStatisticsMapper.xml` with:

```sql
SELECT
  (SELECT COUNT(*) FROM ticket WHERE deleted = 0) AS total_tickets,
  (SELECT COUNT(*) FROM ticket WHERE deleted = 0 AND status IN ('PENDING_ASSIGN', 'PENDING_PROCESS')) AS pending_tickets,
  (SELECT COUNT(*) FROM ticket WHERE deleted = 0 AND status = 'PROCESSING') AS processing_tickets,
  (SELECT COUNT(*) FROM ticket WHERE deleted = 0 AND status = 'RESOLVED') AS resolved_tickets,
  (SELECT COUNT(*) FROM ticket WHERE deleted = 0 AND status = 'CLOSED') AS closed_tickets,
  COALESCE((SELECT AVG((CAST(first_resolved_at AS DATE) - CAST(created_at AS DATE)) * 24)
            FROM ticket
            WHERE deleted = 0 AND first_resolved_at IS NOT NULL), 0) AS average_resolve_hours,
  (SELECT COUNT(*) FROM kb_document WHERE deleted = 0) AS knowledge_documents,
  (SELECT COUNT(*) FROM ai_message WHERE role = 'USER') AS ai_questions,
  COALESCE((SELECT AVG(CASE WHEN max_similarity >= 0.70 THEN 1 ELSE 0 END)
            FROM ai_message
            WHERE role = 'ASSISTANT' AND max_similarity IS NOT NULL), 0) AS knowledge_hit_rate
FROM dual
```

Use category aggregation from `ticket` left joined to `ticket_category`. Use hot question aggregation from `ai_message` where `role = 'USER'`, grouping by `LOWER(TRIM(content))`.

- [x] **Step 5: Write service tests**

Create `AdminStatisticsServiceTest` with a fake mapper. Assert `overview()` returns mapper data and `ticketCategoryStats(0)` / `hotQuestions(0)` normalize limits to `10`, while values over `50` cap at `50`.

- [x] **Step 6: Run service test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=AdminStatisticsServiceTest test
```

Expected: fail because `AdminStatisticsService` does not exist.

- [x] **Step 7: Implement service**

Create `AdminStatisticsService` with methods:

```java
public AdminDashboardOverview overview()
public List<TicketCategoryStat> ticketCategoryStats(int limit)
public List<HotQuestionStat> hotQuestions(int limit)
```

Normalize list limits with default `10` and max `50`.

- [x] **Step 8: Run focused Task 1 tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=AdminStatisticsMapperXmlTest,AdminStatisticsServiceTest test
```

- [x] **Step 9: Commit Task 1**

```bash
git add backend/src/main/java/com/example/aiticket/admin/statistics backend/src/main/resources/mapper/AdminStatisticsMapper.xml backend/src/test/java/com/example/aiticket/admin/statistics docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md
git commit -m "feat: add admin statistics read model"
```

## Task 2: Admin Statistics REST API ⭐

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/web/AdminStatisticsController.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/web/AdminDashboardOverviewResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/web/TicketCategoryStatResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/admin/statistics/web/HotQuestionStatResponse.java`
- Test: `backend/src/test/java/com/example/aiticket/admin/statistics/web/AdminStatisticsControllerTest.java`

- [x] **Step 1: Write controller tests**

Assert these methods have `@PreAuthorize("hasAuthority('dashboard:view')")`:

```text
overview
ticketCategoryStats
hotQuestions
```

Assert response records expose no raw SQL naming and map:

```text
GET /api/admin/statistics/overview
GET /api/admin/statistics/ticket-categories
GET /api/admin/statistics/hot-questions
```

- [x] **Step 2: Run controller test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=AdminStatisticsControllerTest test
```

Expected: fail because controller and response records do not exist.

- [x] **Step 3: Implement response records and controller**

Use `ApiResponse.ok(...)`. Keep `limit` optional with default handled by service:

```java
@GetMapping("/overview")
@PreAuthorize("hasAuthority('dashboard:view')")
public ApiResponse<AdminDashboardOverviewResponse> overview()

@GetMapping("/ticket-categories")
@PreAuthorize("hasAuthority('dashboard:view')")
public ApiResponse<List<TicketCategoryStatResponse>> ticketCategoryStats(@RequestParam(defaultValue = "10") int limit)

@GetMapping("/hot-questions")
@PreAuthorize("hasAuthority('dashboard:view')")
public ApiResponse<List<HotQuestionStatResponse>> hotQuestions(@RequestParam(defaultValue = "10") int limit)
```

- [x] **Step 4: Run focused Task 2 tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=AdminStatisticsControllerTest test
```

- [x] **Step 5: Commit Task 2**

```bash
git add backend/src/main/java/com/example/aiticket/admin/statistics/web backend/src/test/java/com/example/aiticket/admin/statistics/web docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md
git commit -m "feat: expose admin statistics api"
```

## Task 3: Ticket Category Management API

**Files:**
- Modify: `backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java`
- Modify: `backend/src/main/resources/mapper/TicketMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/TicketCategoryService.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/TicketCategoryController.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/TicketCategoryResponse.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/CreateTicketCategoryRequest.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/UpdateTicketCategoryRequest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/service/TicketCategoryServiceTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/web/TicketCategoryControllerTest.java`

- [ ] **Step 1: Write failing tests**

Extend `TicketMapperXmlTest` to assert `nextTicketCategoryId`, `insertTicketCategory`, `updateTicketCategory`, `updateTicketCategoryEnabled`, and `listTicketCategories`. Write service tests for create, rename/reorder, enable, disable, and list. Write controller tests for permission `ticket:manage`.

- [ ] **Step 2: Run focused tests to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest,TicketCategoryServiceTest,TicketCategoryControllerTest test
```

Expected: fail because category write methods and web API do not exist.

- [ ] **Step 3: Implement mapper methods**

Add mapper methods:

```java
Long nextTicketCategoryId();

int insertTicketCategory(Long id, String name, Long parentId, int sortOrder, boolean enabled);

int updateTicketCategory(Long id, String name, Long parentId, int sortOrder);

int updateTicketCategoryEnabled(Long id, boolean enabled);

List<TicketCategory> listTicketCategories(boolean includeDisabled);
```

In XML, use server-side `1/0` generation for booleans with `<choose>`.

- [ ] **Step 4: Implement service**

Validate nonblank names. Default `sortOrder` to `0` and `enabled` to `true` on create. Throw `TicketWorkflowException("ticket category name is required")` for blank names.

- [ ] **Step 5: Implement controller**

Expose:

```text
GET  /api/ticket-categories?includeDisabled=false
POST /api/ticket-categories
POST /api/ticket-categories/{id}
POST /api/ticket-categories/{id}/enable
POST /api/ticket-categories/{id}/disable
```

Use `@PreAuthorize("hasAuthority('ticket:manage')")` for all methods.

- [ ] **Step 6: Run focused Task 3 tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=TicketMapperXmlTest,TicketCategoryServiceTest,TicketCategoryControllerTest test
```

- [ ] **Step 7: Commit Task 3**

```bash
git add backend/src/main/java/com/example/aiticket/ticket backend/src/main/resources/mapper/TicketMapper.xml backend/src/test/java/com/example/aiticket/ticket docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md
git commit -m "feat: add ticket category management api"
```

## Task 4: User and Role Administration API

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/system/SystemAdminMapper.java`
- Create: `backend/src/main/resources/mapper/SystemAdminMapper.xml`
- Create: `backend/src/main/java/com/example/aiticket/system/SystemAdminService.java`
- Create: `backend/src/main/java/com/example/aiticket/system/web/SystemAdminController.java`
- Create: `backend/src/main/java/com/example/aiticket/system/web/*.java`
- Test: `backend/src/test/java/com/example/aiticket/system/SystemAdminMapperXmlTest.java`
- Test: `backend/src/test/java/com/example/aiticket/system/SystemAdminServiceTest.java`
- Test: `backend/src/test/java/com/example/aiticket/system/web/SystemAdminControllerTest.java`

- [ ] **Step 1: Write mapper XML and service tests**

Assert XML declares `listUsers`, `listRoles`, `listPermissions`, `listUserRoleIds`, `deleteUserRoles`, `insertUserRole`, and `updateUserStatus`. Service tests cover list users, list roles, disable/enable user, and replace user roles.

- [ ] **Step 2: Run focused tests to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=SystemAdminMapperXmlTest,SystemAdminServiceTest test
```

Expected: fail because system admin mapper/service do not exist.

- [ ] **Step 3: Implement mapper and service**

Use existing RBAC tables. Do not create passwords in this task. Implement:

```java
List<SystemUserSummary> listUsers(int limit);
List<SystemRoleSummary> listRoles();
List<SystemPermissionSummary> listPermissions();
void updateUserStatus(Long userId, String status);
void replaceUserRoles(Long userId, List<Long> roleIds);
```

Normalize `limit` with default `100` and max `200`. Accept only `ACTIVE` and `DISABLED` status values.

- [ ] **Step 4: Write controller tests**

Assert permissions:

```text
GET  /api/admin/users                     system:user:manage
POST /api/admin/users/{userId}/enable     system:user:manage
POST /api/admin/users/{userId}/disable    system:user:manage
POST /api/admin/users/{userId}/roles      system:role:manage
GET  /api/admin/roles                     system:role:manage
GET  /api/admin/permissions               system:role:manage
```

- [ ] **Step 5: Implement controller and response records**

Return `ApiResponse` wrappers and avoid exposing password hashes or token versions in any response.

- [ ] **Step 6: Run focused Task 4 tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=SystemAdminMapperXmlTest,SystemAdminServiceTest,SystemAdminControllerTest test
```

- [ ] **Step 7: Commit Task 4**

```bash
git add backend/src/main/java/com/example/aiticket/system backend/src/main/resources/mapper/SystemAdminMapper.xml backend/src/test/java/com/example/aiticket/system docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md
git commit -m "feat: add system administration api"
```

## Task 5: Full Verification and Phase 6 Progress Marking

**Files:**
- Create: `docs/spikes/phase-6-admin-dashboard.md`
- Modify: `docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md`
- Modify: `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
- Modify: `沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`

- [ ] **Step 1: Run all backend tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
```

- [ ] **Step 2: Run local backend smoke verification**

Login as `admin`, call admin statistics endpoints, ticket category endpoints, and system admin endpoints. Sanitize tokens and do not print JWTs.

- [ ] **Step 3: Verify RBAC**

Verify unauthenticated calls return `401`. Verify ordinary `user` cannot access admin statistics or system admin endpoints and receives `403`.

- [ ] **Step 4: Record spike report**

Write `docs/spikes/phase-6-admin-dashboard.md` with commands, sanitized responses, test result, and any frontend scope intentionally deferred.

- [ ] **Step 5: Final diff and plan marking**

Run:

```bash
git diff --check
git status --short --branch
```

Mark completed tasks with checkboxes and stars.

- [ ] **Step 6: Commit Task 5**

```bash
git add docs/spikes/phase-6-admin-dashboard.md docs/superpowers/plans/2026-06-20-phase-6-admin-dashboard-implementation-plan.md docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md 沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md
git commit -m "docs: verify phase 6 admin dashboard"
```

## Current Execution Note

Phase 6 is planned as a backend-first management and statistics slice. It starts after Phase 5 ticket workflow backend core, flow logs, and ticket comments are complete.
