# Phase 39/40 Assignment Recommendation and SLA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add visible assignment recommendation and priority-based SLA status to the existing ticket workflow without changing the proven manual assignment process.

**Architecture:** SLA is implemented as small deterministic domain services under `ticket.service`, then exposed through existing ticket response DTOs. Assignment recommendation is a read-only backend path that ranks active AGENT users by active workload and lets the existing admin assignment UI consume the recommendation without mutating ticket state.

**Tech Stack:** Spring Boot 3, Java 21 records/enums, MyBatis XML mapper, Vue 3 Composition API, Pinia auth store, Vitest, Maven Surefire.

---

## File Structure

- Create `backend/src/main/java/com/example/aiticket/ticket/domain/SlaStatus.java`
  - Enum for `COMPLETED`, `OVERDUE`, `DUE_SOON`, `ON_TRACK`.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/SlaSnapshot.java`
  - Record carrying `deadlineAt`, `slaStatus`, `slaRemainingMinutes`.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/SlaPolicy.java`
  - Maps `TicketPriority` to deadline duration and calculates `deadlineAt`.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/SlaCalculator.java`
  - Derives `SlaSnapshot` from ticket status, deadline, and current time.
- Create `backend/src/test/java/com/example/aiticket/ticket/service/SlaPolicyTest.java`
- Create `backend/src/test/java/com/example/aiticket/ticket/service/SlaCalculatorTest.java`
- Modify `backend/src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java`
  - Use `SlaPolicy` when creating tickets.
- Modify `backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java`
  - Assert priority-based deadline persistence.
- Modify `backend/src/main/java/com/example/aiticket/ticket/web/TicketResponse.java`
  - Add SLA fields.
- Modify `backend/src/main/java/com/example/aiticket/ticket/web/TicketDetailResponse.java`
  - Add SLA fields by delegating through `TicketResponse`.
- Create `backend/src/test/java/com/example/aiticket/ticket/web/TicketResponseTest.java`
  - Protect DTO SLA derivation without spinning up MVC.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/AgentWorkload.java`
  - Read model for candidate agent workload.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/AssignmentRecommendation.java`
  - API/service record for recommendation result.
- Create `backend/src/main/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationService.java`
  - Read-only recommendation service.
- Create `backend/src/test/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationServiceTest.java`
- Modify `backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java`
  - Add `listAgentWorkloads()`.
- Modify `backend/src/main/resources/mapper/TicketMapper.xml`
  - Add workload query.
- Modify `backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java`
  - Assert mapper XML contains status values and AGENT role filter.
- Create `backend/src/main/java/com/example/aiticket/ticket/web/AssignmentRecommendationResponse.java`
- Modify `backend/src/main/java/com/example/aiticket/ticket/web/TicketController.java`
  - Add recommendation endpoint.
- Modify `backend/src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java`
  - Assert permission and response shape.
- Modify `frontend/src/api/tickets.ts`
  - Add SLA fields, recommendation type, and API function.
- Modify `frontend/src/api/tickets.spec.ts`
  - Assert recommendation endpoint path.
- Modify `frontend/src/views/tickets/TicketListView.vue`
  - Add SLA column/chip and deadline text.
- Modify `frontend/src/views/tickets/TicketListView.spec.ts`
  - Assert SLA labels without depending on live minute values.
- Modify `frontend/src/views/tickets/TicketDetailView.vue`
  - Add SLA panel and assignment recommendation panel.
- Modify `frontend/src/views/tickets/TicketDetailView.spec.ts`
  - Assert recommendation UI, use-recommendation button, and SLA panel.
- Add `docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md`
  - Mark each completed task with `[x] ⭐` during execution.

---

### Task 1: SLA Domain Policy and Calculator

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/ticket/domain/SlaStatus.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/SlaSnapshot.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/SlaPolicy.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/SlaCalculator.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/service/SlaPolicyTest.java`
- Test: `backend/src/test/java/com/example/aiticket/ticket/service/SlaCalculatorTest.java`

- [x] ⭐ **Step 1: Write failing SLA policy tests**

Create `SlaPolicyTest.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketPriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyTest {
    private final SlaPolicy policy = new SlaPolicy();
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 23, 9, 0);

    @Test
    void calculatesDeadlineByPriority() {
        assertThat(policy.deadlineFor(TicketPriority.URGENT, createdAt)).isEqualTo(createdAt.plusHours(4));
        assertThat(policy.deadlineFor(TicketPriority.HIGH, createdAt)).isEqualTo(createdAt.plusHours(8));
        assertThat(policy.deadlineFor(TicketPriority.NORMAL, createdAt)).isEqualTo(createdAt.plusHours(24));
        assertThat(policy.deadlineFor(TicketPriority.LOW, createdAt)).isEqualTo(createdAt.plusHours(72));
    }

    @Test
    void defaultsMissingPriorityToNormal() {
        assertThat(policy.deadlineFor(null, createdAt)).isEqualTo(createdAt.plusHours(24));
    }
}
```

- [x] ⭐ **Step 2: Write failing SLA calculator tests**

Create `SlaCalculatorTest.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlaCalculatorTest {
    private final SlaCalculator calculator = new SlaCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 23, 10, 0);

    @Test
    void treatsResolvedAndClosedAsCompletedWithNoRemainingMinutes() {
        LocalDateTime deadline = now.plusHours(3);

        assertThat(calculator.snapshot(TicketStatus.RESOLVED, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.COMPLETED, null));
        assertThat(calculator.snapshot(TicketStatus.CLOSED, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.COMPLETED, null));
    }

    @Test
    void handlesMissingDeadlineAsOnTrackWithoutRemainingMinutes() {
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, null, now))
                .isEqualTo(new SlaSnapshot(null, SlaStatus.ON_TRACK, null));
    }

    @Test
    void classifiesOverdueOnlyWhenNowIsAfterDeadline() {
        LocalDateTime deadline = now.minusMinutes(1);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now).slaStatus())
                .isEqualTo(SlaStatus.OVERDUE);
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now).slaRemainingMinutes())
                .isEqualTo(-1L);
    }

    @Test
    void classifiesExactDeadlineAsDueSoon() {
        assertThat(calculator.snapshot(TicketStatus.PROCESSING, now, now))
                .isEqualTo(new SlaSnapshot(now, SlaStatus.DUE_SOON, 0L));
    }

    @Test
    void classifiesExactlyTwoHoursRemainingAsDueSoon() {
        LocalDateTime deadline = now.plusMinutes(120);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.DUE_SOON, 120L));
    }

    @Test
    void classifiesMoreThanTwoHoursRemainingAsOnTrack() {
        LocalDateTime deadline = now.plusMinutes(121);

        assertThat(calculator.snapshot(TicketStatus.PROCESSING, deadline, now))
                .isEqualTo(new SlaSnapshot(deadline, SlaStatus.ON_TRACK, 121L));
    }
}
```

- [x] ⭐ **Step 3: Run tests and verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Dtest=SlaPolicyTest,SlaCalculatorTest test
```

Expected: FAIL because `SlaPolicy`, `SlaCalculator`, `SlaSnapshot`, and `SlaStatus` do not exist.

- [x] ⭐ **Step 4: Implement SLA domain classes**

Create `SlaStatus.java`:

```java
package com.example.aiticket.ticket.domain;

public enum SlaStatus {
    COMPLETED,
    OVERDUE,
    DUE_SOON,
    ON_TRACK
}
```

Create `SlaSnapshot.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;

import java.time.LocalDateTime;

public record SlaSnapshot(
        LocalDateTime deadlineAt,
        SlaStatus slaStatus,
        Long slaRemainingMinutes
) {
}
```

Create `SlaPolicy.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.TicketPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaPolicy {
    public LocalDateTime deadlineFor(TicketPriority priority, LocalDateTime createdAt) {
        return createdAt.plus(durationFor(priority));
    }

    public Duration durationFor(TicketPriority priority) {
        TicketPriority normalized = priority == null ? TicketPriority.NORMAL : priority;
        return switch (normalized) {
            case URGENT -> Duration.ofHours(4);
            case HIGH -> Duration.ofHours(8);
            case NORMAL -> Duration.ofHours(24);
            case LOW -> Duration.ofHours(72);
        };
    }
}
```

Create `SlaCalculator.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaCalculator {
    private static final long DUE_SOON_MINUTES = 120L;

    public SlaSnapshot snapshot(TicketStatus status, LocalDateTime deadlineAt) {
        return snapshot(status, deadlineAt, LocalDateTime.now());
    }

    SlaSnapshot snapshot(TicketStatus status, LocalDateTime deadlineAt, LocalDateTime now) {
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            return new SlaSnapshot(deadlineAt, SlaStatus.COMPLETED, null);
        }
        if (deadlineAt == null) {
            return new SlaSnapshot(null, SlaStatus.ON_TRACK, null);
        }

        long remainingMinutes = Duration.between(now, deadlineAt).toMinutes();
        if (now.isAfter(deadlineAt)) {
            return new SlaSnapshot(deadlineAt, SlaStatus.OVERDUE, remainingMinutes);
        }
        if (remainingMinutes <= DUE_SOON_MINUTES) {
            return new SlaSnapshot(deadlineAt, SlaStatus.DUE_SOON, remainingMinutes);
        }
        return new SlaSnapshot(deadlineAt, SlaStatus.ON_TRACK, remainingMinutes);
    }
}
```

- [x] ⭐ **Step 5: Run tests and verify GREEN**

Run the same Maven command from Step 3.

Expected: PASS.

- [x] ⭐ **Step 6: Update plan checkbox**

In this file, change Task 1 checkbox from `[ ]` to `[x] ⭐` after tests pass.

- [x] ⭐ **Step 7: Commit**

```bash
git add backend/src/main/java/com/example/aiticket/ticket/domain/SlaStatus.java \
  backend/src/main/java/com/example/aiticket/ticket/service/SlaSnapshot.java \
  backend/src/main/java/com/example/aiticket/ticket/service/SlaPolicy.java \
  backend/src/main/java/com/example/aiticket/ticket/service/SlaCalculator.java \
  backend/src/test/java/com/example/aiticket/ticket/service/SlaPolicyTest.java \
  backend/src/test/java/com/example/aiticket/ticket/service/SlaCalculatorTest.java \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: add ticket sla policy"
```

---

### Task 2: Persist SLA Deadline and Expose SLA Fields

**Files:**
- Modify: `backend/src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java`
- Modify: `backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/web/TicketResponse.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/web/TicketDetailResponse.java`
- Create: `backend/src/test/java/com/example/aiticket/ticket/web/TicketResponseTest.java`

- [x] ⭐ **Step 1: Write failing workflow deadline test**

In `TicketWorkflowServiceTest#createFromAiSessionInsertsTicketAndCreateFlowLog`, add:

```java
assertThat(ticket.deadlineAt()).isNotNull();
assertThat(mapper.insertedTickets.getFirst().deadlineAt()).isNotNull();
```

Add a focused test:

```java
@Test
void createFromAiSessionPersistsDeadlineFromPriority() {
    FakeTicketMapper mapper = new FakeTicketMapper();
    TicketWorkflowService service = new TicketWorkflowService(mapper, new ManualAssignmentStrategy());

    Ticket urgent = service.createFromAiSession(7L, "USER", new CreateTicketFromAiSessionCommand(
            10L, null, "紧急问题", "系统完全不可用。", 1L, TicketPriority.URGENT, "紧急处理"
    ));

    assertThat(urgent.deadlineAt()).isNotNull();
    assertThat(mapper.insertedTickets.getFirst().deadlineAt()).isEqualTo(urgent.deadlineAt());
}
```

- [x] ⭐ **Step 2: Write failing DTO SLA test**

Create `TicketResponseTest.java`:

```java
package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.SlaStatus;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketResponseTest {
    @Test
    void exposesDerivedSlaFields() {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(90);
        Ticket ticket = new Ticket(8L, "TK8", "标题", "描述", TicketStatus.PROCESSING,
                TicketPriority.HIGH, null, 1L, null, 4L, 3L, TicketSource.AI_SESSION,
                10L, 21L, "摘要", "建议", "原因", deadline, null, null, 0, false,
                LocalDateTime.now(), LocalDateTime.now());

        TicketResponse response = TicketResponse.from(ticket);

        assertThat(response.deadlineAt()).isEqualTo(deadline);
        assertThat(response.slaStatus()).isEqualTo(SlaStatus.DUE_SOON);
        assertThat(response.slaRemainingMinutes()).isNotNull();
    }
}
```

- [x] ⭐ **Step 3: Run tests and verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Dtest=TicketWorkflowServiceTest,TicketResponseTest test
```

Expected: FAIL because `TicketResponse` lacks SLA accessors and workflow creation still passes null deadline.

- [x] ⭐ **Step 4: Implement workflow deadline calculation**

Modify `TicketWorkflowService`:

```java
private final SlaPolicy slaPolicy;

public TicketWorkflowService(TicketMapper mapper, AssignmentStrategy assignmentStrategy) {
    this(mapper, assignmentStrategy, new SlaPolicy());
}

public TicketWorkflowService(TicketMapper mapper, AssignmentStrategy assignmentStrategy, SlaPolicy slaPolicy) {
    this.mapper = mapper;
    this.assignmentStrategy = assignmentStrategy;
    this.slaPolicy = slaPolicy;
}
```

In `createFromAiSession`, before `mapper.insertTicket(...)`:

```java
LocalDateTime now = LocalDateTime.now();
LocalDateTime deadlineAt = slaPolicy.deadlineFor(priority, now);
```

Replace the `null` deadline argument with `deadlineAt`, and return ticket timestamps using `now`:

```java
deadlineAt,
null, null, 0, false, now, now
```

- [x] ⭐ **Step 5: Implement response SLA fields**

Modify `TicketResponse` record fields to include:

```java
LocalDateTime deadlineAt,
SlaStatus slaStatus,
Long slaRemainingMinutes,
```

Import `SlaStatus`, `SlaCalculator`, and `SlaSnapshot`.

In `from(Ticket ticket)`:

```java
SlaSnapshot sla = new SlaCalculator().snapshot(ticket.status(), ticket.deadlineAt());
```

Pass these fields after `transferReason`:

```java
ticket.deadlineAt(),
sla.slaStatus(),
sla.slaRemainingMinutes(),
```

Modify `TicketDetailResponse` to include the same fields and populate them from `TicketResponse`.

- [x] ⭐ **Step 6: Run tests and verify GREEN**

Run the command from Step 3.

Expected: PASS.

- [x] ⭐ **Step 7: Update plan checkbox**

Mark Task 2 `[x] ⭐`.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java \
  backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java \
  backend/src/main/java/com/example/aiticket/ticket/web/TicketResponse.java \
  backend/src/main/java/com/example/aiticket/ticket/web/TicketDetailResponse.java \
  backend/src/test/java/com/example/aiticket/ticket/web/TicketResponseTest.java \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: expose ticket sla status"
```

---

### Task 3: Assignment Recommendation Backend

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/AgentWorkload.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/AssignmentRecommendation.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationService.java`
- Create: `backend/src/test/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationServiceTest.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java`
- Modify: `backend/src/main/resources/mapper/TicketMapper.xml`
- Modify: `backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java`

- [ ] **Step 1: Write failing recommendation service tests**

Create `TicketAssignmentRecommendationServiceTest.java` with a fake mapper implementing only methods used by the service:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.*;
import com.example.aiticket.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAssignmentRecommendationServiceTest {
    @Test
    void recommendsLowestLoadActiveAgent() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.workloads = List.of(
                new AgentWorkload(3L, "agent", "演示坐席", 4L),
                new AgentWorkload(5L, "agent2", "二线坐席", 1L)
        );
        TicketAssignmentRecommendationService service = new TicketAssignmentRecommendationService(mapper);

        AssignmentRecommendation recommendation = service.recommend(100L);

        assertThat(recommendation.recommendedAssigneeId()).isEqualTo(5L);
        assertThat(recommendation.activeTicketCount()).isEqualTo(1L);
        assertThat(recommendation.reason()).isEqualTo("推荐二线坐席：当前在办 1 单，是当前负载最低坐席");
    }

    @Test
    void breaksLoadTiesByUserId() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.workloads = List.of(
                new AgentWorkload(5L, "agent2", "二线坐席", 1L),
                new AgentWorkload(3L, "agent", "演示坐席", 1L)
        );
        TicketAssignmentRecommendationService service = new TicketAssignmentRecommendationService(mapper);

        assertThat(service.recommend(100L).recommendedAssigneeId()).isEqualTo(3L);
    }

    @Test
    void returnsEmptyRecommendationWhenNoActiveAgentExists() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.workloads = List.of();
        TicketAssignmentRecommendationService service = new TicketAssignmentRecommendationService(mapper);

        AssignmentRecommendation recommendation = service.recommend(100L);

        assertThat(recommendation.recommendedAssigneeId()).isNull();
        assertThat(recommendation.reason()).isEqualTo("暂无可推荐坐席");
    }

    @Test
    void returnsEmptyRecommendationForNonPendingAssignTicket() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticket = mapper.ticket(TicketStatus.PROCESSING);
        TicketAssignmentRecommendationService service = new TicketAssignmentRecommendationService(mapper);

        AssignmentRecommendation recommendation = service.recommend(100L);

        assertThat(recommendation.recommendedAssigneeId()).isNull();
        assertThat(recommendation.reason()).isEqualTo("当前状态不需要分派");
    }

    @Test
    void raisesNotFoundWhenTicketDoesNotExist() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticket = null;
        TicketAssignmentRecommendationService service = new TicketAssignmentRecommendationService(mapper);

        assertThatThrownBy(() -> service.recommend(100L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("ticket not found");
    }

    @Test
    void formatsReasonForOnlyCandidateAndZeroWorkload() {
        assertThat(TicketAssignmentRecommendationService.reason(new AgentWorkload(3L, "agent", "演示坐席", 2L), 1))
                .isEqualTo("推荐演示坐席：当前在办 2 单，是当前唯一可用坐席");
        assertThat(TicketAssignmentRecommendationService.reason(new AgentWorkload(3L, "agent", "演示坐席", 0L), 2))
                .isEqualTo("推荐演示坐席：当前无在办工单，可优先承接");
    }

    private static final class FakeTicketMapper implements TicketMapper {
        private Ticket ticket = ticket(TicketStatus.PENDING_ASSIGN);
        private List<AgentWorkload> workloads = new ArrayList<>();

        private Ticket ticket(TicketStatus status) {
            return new Ticket(100L, "TK100", "标题", "描述", status, TicketPriority.NORMAL,
                    null, 1L, null, 7L, null, TicketSource.AI_SESSION, 10L, 21L,
                    "摘要", "建议", "原因", null, null, null, 0, false,
                    LocalDateTime.now(), LocalDateTime.now());
        }

        @Override public Ticket findTicketById(Long ticketId) { return ticket; }
        @Override public List<AgentWorkload> listAgentWorkloads() { return workloads; }

        // Other TicketMapper methods can return neutral values because this fake is scoped to recommendation tests.
        @Override public Long nextTicketId() { return 0L; }
        @Override public Long nextFlowLogId() { return 0L; }
        @Override public Long nextCommentId() { return 0L; }
        @Override public Long nextTicketCategoryId() { return 0L; }
        @Override public AiSession findOwnedAiSession(Long sessionId, Long userId) { return null; }
        @Override public AiMessage findLatestAssistantMessage(Long sessionId, Long userId) { return null; }
        @Override public AiMessage findOwnedAssistantMessage(Long messageId, Long sessionId, Long userId) { return null; }
        @Override public int insertTicket(Long id, String ticketNo, String title, String description, TicketStatus status, TicketPriority priority, String aiPrioritySuggestion, Long categoryId, Long departmentId, Long creatorId, Long assigneeId, TicketSource source, Long sourceSessionId, Long sourceMessageId, String aiSummary, String aiSuggestion, String transferReason, LocalDateTime deadlineAt) { return 0; }
        @Override public int insertFlowLog(Long id, Long ticketId, TicketStatus fromStatus, TicketStatus toStatus, TicketWorkflowAction action, Long operatorId, String operatorRole, String commentText) { return 0; }
        @Override public Ticket findTicketForUpdate(Long ticketId) { return null; }
        @Override public Ticket findCreatedTicket(Long ticketId, Long creatorId) { return null; }
        @Override public Ticket findAssignedTicket(Long ticketId, Long assigneeId) { return null; }
        @Override public List<Ticket> listCreatedTickets(Long creatorId, int limit) { return List.of(); }
        @Override public List<Ticket> listAssignedTickets(Long assigneeId, int limit) { return List.of(); }
        @Override public List<Ticket> listManagedTickets(int limit) { return List.of(); }
        @Override public List<TicketFlowLog> listFlowLogs(Long ticketId) { return List.of(); }
        @Override public int insertComment(Long id, Long ticketId, Long authorId, TicketCommentType commentType, String content, boolean internal) { return 0; }
        @Override public List<TicketComment> listComments(Long ticketId, boolean includeInternal) { return List.of(); }
        @Override public int insertTicketCategory(Long id, String name, Long parentId, int sortOrder, boolean enabled) { return 0; }
        @Override public int updateTicketCategory(Long id, String name, Long parentId, int sortOrder) { return 0; }
        @Override public int updateTicketCategoryEnabled(Long id, boolean enabled) { return 0; }
        @Override public List<TicketCategory> listTicketCategories(boolean includeDisabled) { return List.of(); }
        @Override public int updateTicketStatus(Long ticketId, TicketStatus status, Long assigneeId, LocalDateTime firstResolvedAt, LocalDateTime closedAt, Integer incrementReopen) { return 0; }
    }
}
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Dtest=TicketAssignmentRecommendationServiceTest test
```

Expected: FAIL because recommendation classes and mapper method do not exist.

- [ ] **Step 3: Implement recommendation records and service**

Create `AgentWorkload.java`:

```java
package com.example.aiticket.ticket.service;

public record AgentWorkload(
        Long userId,
        String username,
        String displayName,
        Long activeTicketCount
) {
}
```

Create `AssignmentRecommendation.java`:

```java
package com.example.aiticket.ticket.service;

public record AssignmentRecommendation(
        Long recommendedAssigneeId,
        String recommendedUsername,
        String recommendedDisplayName,
        Long activeTicketCount,
        String reason
) {
    public static AssignmentRecommendation empty(String reason) {
        return new AssignmentRecommendation(null, null, null, null, reason);
    }
}
```

Create `TicketAssignmentRecommendationService.java`:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TicketAssignmentRecommendationService {
    private final TicketMapper mapper;

    public TicketAssignmentRecommendationService(TicketMapper mapper) {
        this.mapper = mapper;
    }

    public AssignmentRecommendation recommend(Long ticketId) {
        Ticket ticket = mapper.findTicketById(ticketId);
        if (ticket == null) {
            throw new TicketNotFoundException("ticket not found");
        }
        if (ticket.status() != TicketStatus.PENDING_ASSIGN) {
            return AssignmentRecommendation.empty("当前状态不需要分派");
        }

        List<AgentWorkload> workloads = mapper.listAgentWorkloads();
        if (workloads.isEmpty()) {
            return AssignmentRecommendation.empty("暂无可推荐坐席");
        }

        AgentWorkload selected = workloads.stream()
                .min(Comparator.comparing(AgentWorkload::activeTicketCount).thenComparing(AgentWorkload::userId))
                .orElseThrow();
        return new AssignmentRecommendation(selected.userId(), selected.username(), selected.displayName(),
                selected.activeTicketCount(), reason(selected, workloads.size()));
    }

    static String reason(AgentWorkload workload, int candidateCount) {
        if (candidateCount == 1) {
            return "推荐%s：当前在办 %d 单，是当前唯一可用坐席"
                    .formatted(workload.displayName(), workload.activeTicketCount());
        }
        if (workload.activeTicketCount() == 0L) {
            return "推荐%s：当前无在办工单，可优先承接".formatted(workload.displayName());
        }
        return "推荐%s：当前在办 %d 单，是当前负载最低坐席"
                .formatted(workload.displayName(), workload.activeTicketCount());
    }
}
```

- [ ] **Step 4: Add mapper method and XML query**

In `TicketMapper.java`, add:

```java
List<AgentWorkload> listAgentWorkloads();
```

Import `AgentWorkload`.

In `TicketMapper.xml`, add:

```xml
<select id="listAgentWorkloads" resultType="com.example.aiticket.ticket.service.AgentWorkload">
    SELECT u.id AS user_id,
           u.username,
           u.display_name,
           COALESCE(COUNT(t.id), 0) AS active_ticket_count
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.id
    JOIN sys_role r ON r.id = ur.role_id
                   AND r.role_code = 'AGENT'
                   AND r.status = 'ACTIVE'
    LEFT JOIN ticket t ON t.assignee_id = u.id
                      AND t.deleted = 0
                      AND t.status IN ('PENDING_PROCESS', 'PROCESSING', 'RESOLVED')
    WHERE u.status = 'ACTIVE'
    GROUP BY u.id, u.username, u.display_name
    ORDER BY active_ticket_count ASC, u.id ASC
</select>
```

- [ ] **Step 5: Add mapper XML test coverage**

In `TicketMapperXmlTest`, add assertions:

```java
assertThat(mapper).contains("listAgentWorkloads");
assertThat(mapper).contains("r.role_code = 'AGENT'");
assertThat(mapper).contains("t.status IN ('PENDING_PROCESS', 'PROCESSING', 'RESOLVED')");
assertThat(mapper).contains("ORDER BY active_ticket_count ASC, u.id ASC");
```

- [ ] **Step 6: Run tests and verify GREEN**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Dtest=TicketAssignmentRecommendationServiceTest,TicketMapperXmlTest test
```

Expected: PASS.

- [ ] **Step 7: Update plan checkbox**

Mark Task 3 `[x] ⭐`.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/example/aiticket/ticket/service/AgentWorkload.java \
  backend/src/main/java/com/example/aiticket/ticket/service/AssignmentRecommendation.java \
  backend/src/main/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationService.java \
  backend/src/test/java/com/example/aiticket/ticket/service/TicketAssignmentRecommendationServiceTest.java \
  backend/src/main/java/com/example/aiticket/ticket/mapper/TicketMapper.java \
  backend/src/main/resources/mapper/TicketMapper.xml \
  backend/src/test/java/com/example/aiticket/ticket/mapper/TicketMapperXmlTest.java \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: recommend ticket assignee"
```

---

### Task 4: Recommendation API Endpoint

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/ticket/web/AssignmentRecommendationResponse.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/web/TicketController.java`
- Modify: `backend/src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

In `TicketControllerTest`, add:

```java
@Test
void recommendationEndpointRequiresTicketAssignPermission() throws NoSuchMethodException {
    assertThat(method("assignmentRecommendation", Long.class).getAnnotation(PreAuthorize.class).value())
            .isEqualTo("hasAuthority('ticket:assign')");
}

@Test
void recommendationEndpointReturnsRecommendationResponse() {
    FakeRecommendationService recommendationService = new FakeRecommendationService();
    TicketController controller = new TicketController(new FakeTicketWorkflowService(), recommendationService);

    AssignmentRecommendationResponse response = controller.assignmentRecommendation(100L).data();

    assertThat(response.recommendedAssigneeId()).isEqualTo(3L);
    assertThat(response.recommendedUsername()).isEqualTo("agent");
    assertThat(response.recommendedDisplayName()).isEqualTo("演示坐席");
    assertThat(response.activeTicketCount()).isEqualTo(2L);
    assertThat(response.reason()).contains("演示坐席");
}
```

Add a fake recommendation service in the test file:

```java
private static final class FakeRecommendationService extends TicketAssignmentRecommendationService {
    private FakeRecommendationService() {
        super(null);
    }

    @Override
    public AssignmentRecommendation recommend(Long ticketId) {
        return new AssignmentRecommendation(3L, "agent", "演示坐席", 2L,
                "推荐演示坐席：当前在办 2 单，是当前负载最低坐席");
    }
}
```

- [ ] **Step 2: Run test and verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Dtest=TicketControllerTest test
```

Expected: FAIL because constructor and endpoint do not exist.

- [ ] **Step 3: Implement response record**

Create `AssignmentRecommendationResponse.java`:

```java
package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.service.AssignmentRecommendation;

public record AssignmentRecommendationResponse(
        Long recommendedAssigneeId,
        String recommendedUsername,
        String recommendedDisplayName,
        Long activeTicketCount,
        String reason
) {
    public static AssignmentRecommendationResponse from(AssignmentRecommendation recommendation) {
        return new AssignmentRecommendationResponse(
                recommendation.recommendedAssigneeId(),
                recommendation.recommendedUsername(),
                recommendation.recommendedDisplayName(),
                recommendation.activeTicketCount(),
                recommendation.reason()
        );
    }
}
```

- [ ] **Step 4: Implement controller endpoint**

Modify `TicketController`:

```java
private final TicketAssignmentRecommendationService recommendationService;

public TicketController(TicketWorkflowService service, TicketAssignmentRecommendationService recommendationService) {
    this.service = service;
    this.recommendationService = recommendationService;
}
```

Add endpoint:

```java
@GetMapping("/{ticketId}/assignment-recommendation")
@PreAuthorize("hasAuthority('ticket:assign')")
public ApiResponse<AssignmentRecommendationResponse> assignmentRecommendation(@PathVariable Long ticketId) {
    return ApiResponse.ok(AssignmentRecommendationResponse.from(recommendationService.recommend(ticketId)));
}
```

- [ ] **Step 5: Update tests using controller constructor**

Replace existing `new TicketController(new FakeTicketWorkflowService())` calls with:

```java
new TicketController(new FakeTicketWorkflowService(), new FakeRecommendationService())
```

- [ ] **Step 6: Run test and verify GREEN**

Run the command from Step 2.

Expected: PASS.

- [ ] **Step 7: Update plan checkbox**

Mark Task 4 `[x] ⭐`.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/example/aiticket/ticket/web/AssignmentRecommendationResponse.java \
  backend/src/main/java/com/example/aiticket/ticket/web/TicketController.java \
  backend/src/test/java/com/example/aiticket/ticket/web/TicketControllerTest.java \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: expose assignment recommendation api"
```

---

### Task 5: Frontend API Types and Ticket List SLA

**Files:**
- Modify: `frontend/src/api/tickets.ts`
- Modify: `frontend/src/api/tickets.spec.ts`
- Modify: `frontend/src/views/tickets/TicketListView.vue`
- Modify: `frontend/src/views/tickets/TicketListView.spec.ts`

- [ ] **Step 1: Write failing API test**

In `tickets.spec.ts`, import `getAssignmentRecommendation` and add:

```ts
it('loads assignment recommendation for a ticket', async () => {
  getMock.mockResolvedValueOnce({
    data: {
      success: true,
      data: {
        recommendedAssigneeId: 3,
        recommendedUsername: 'agent',
        recommendedDisplayName: '演示坐席',
        activeTicketCount: 2,
        reason: '推荐演示坐席：当前在办 2 单，是当前负载最低坐席'
      },
      message: 'ok'
    }
  })

  await expect(getAssignmentRecommendation(8)).resolves.toMatchObject({
    recommendedAssigneeId: 3,
    recommendedUsername: 'agent'
  })
  expect(getMock).toHaveBeenCalledWith('/tickets/8/assignment-recommendation')
})
```

- [ ] **Step 2: Write failing ticket list SLA test**

In `TicketListView.spec.ts`, add SLA fields to at least one ticket:

```ts
deadlineAt: '2026-06-20T18:00:00',
slaStatus: 'DUE_SOON',
slaRemainingMinutes: 90
```

Add expectations:

```ts
expect(wrapper.text()).toContain('SLA')
expect(wrapper.text()).toContain('即将超时')
expect(wrapper.text()).toContain('2026-06-20 18:00')
```

- [ ] **Step 3: Run frontend focused tests and verify RED**

```bash
cd frontend
npm run test -- src/api/tickets.spec.ts src/views/tickets/TicketListView.spec.ts
```

Expected: FAIL because API function and SLA UI are missing.

- [ ] **Step 4: Implement frontend API types**

In `tickets.ts`, add:

```ts
export type SlaStatus = 'COMPLETED' | 'OVERDUE' | 'DUE_SOON' | 'ON_TRACK'
```

Add to `TicketSummary`:

```ts
deadlineAt?: string | null
slaStatus?: SlaStatus | null
slaRemainingMinutes?: number | null
```

Add:

```ts
export interface AssignmentRecommendation {
  recommendedAssigneeId?: number | null
  recommendedUsername?: string | null
  recommendedDisplayName?: string | null
  activeTicketCount?: number | null
  reason: string
}

export async function getAssignmentRecommendation(ticketId: number): Promise<AssignmentRecommendation> {
  const response = await http.get<ApiResponse<AssignmentRecommendation>>(
    `/tickets/${ticketId}/assignment-recommendation`
  )

  return unwrapData(response.data)
}
```

- [ ] **Step 5: Implement ticket list SLA UI**

In `TicketListView.vue`, import `type SlaStatus`.

Add labels:

```ts
const slaLabel: Record<SlaStatus, string> = {
  ON_TRACK: '正常',
  DUE_SOON: '即将超时',
  OVERDUE: '已超时',
  COMPLETED: '已完成'
}

function formatSla(status?: SlaStatus | null) {
  return status ? slaLabel[status] : '未设置'
}
```

Add `SLA` column to the header and row:

```vue
<span>SLA</span>
```

```vue
<span>
  <mark class="ticket-chip" :class="`sla-${ticket.slaStatus || 'unknown'}`">{{ formatSla(ticket.slaStatus) }}</mark>
  <small v-if="ticket.deadlineAt">{{ formatDate(ticket.deadlineAt) }}</small>
</span>
```

- [ ] **Step 6: Run focused tests and verify GREEN**

Run command from Step 3.

Expected: PASS.

- [ ] **Step 7: Update plan checkbox**

Mark Task 5 `[x] ⭐`.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/api/tickets.ts frontend/src/api/tickets.spec.ts \
  frontend/src/views/tickets/TicketListView.vue frontend/src/views/tickets/TicketListView.spec.ts \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: show ticket sla in lists"
```

---

### Task 6: Frontend Ticket Detail Recommendation and SLA Panel

**Files:**
- Modify: `frontend/src/views/tickets/TicketDetailView.vue`
- Modify: `frontend/src/views/tickets/TicketDetailView.spec.ts`

- [ ] **Step 1: Write failing detail tests**

In `TicketDetailView.spec.ts`, mock `getAssignmentRecommendation`:

```ts
getAssignmentRecommendation: vi.fn()
```

Import and reset it:

```ts
import { getAssignmentRecommendation } from '../../api/tickets'
const getAssignmentRecommendationMock = vi.mocked(getAssignmentRecommendation)
```

In the admin assignment test, set ticket SLA fields:

```ts
deadlineAt: '2026-06-20T18:00:00',
slaStatus: 'DUE_SOON',
slaRemainingMinutes: 90
```

Mock recommendation:

```ts
getAssignmentRecommendationMock.mockResolvedValue({
  recommendedAssigneeId: 3,
  recommendedUsername: 'agent',
  recommendedDisplayName: '演示坐席',
  activeTicketCount: 0,
  reason: '推荐演示坐席：当前无在办工单，可优先承接'
})
```

Add expectations before selecting assignee manually:

```ts
expect(wrapper.text()).toContain('智能推荐')
expect(wrapper.text()).toContain('推荐演示坐席：当前无在办工单，可优先承接')
expect(wrapper.text()).toContain('SLA 状态')
expect(wrapper.text()).toContain('即将超时')
expect(wrapper.text()).toContain('2026-06-20 18:00')

await wrapper.find('[data-testid="use-recommended-assignee"]').trigger('click')
expect((wrapper.find('[data-testid="assignee-select"]').element as HTMLSelectElement).value).toBe('3')
```

- [ ] **Step 2: Run focused test and verify RED**

```bash
cd frontend
npm run test -- src/views/tickets/TicketDetailView.spec.ts
```

Expected: FAIL because recommendation API is not called and UI is missing.

- [ ] **Step 3: Implement detail state and recommendation load**

In `TicketDetailView.vue`, import:

```ts
getAssignmentRecommendation,
type AssignmentRecommendation,
type SlaStatus
```

Add state:

```ts
const recommendation = ref<AssignmentRecommendation | null>(null)
const loadingRecommendation = ref(false)
```

Add SLA helpers:

```ts
const slaLabel: Record<SlaStatus, string> = {
  ON_TRACK: '正常',
  DUE_SOON: '即将超时',
  OVERDUE: '已超时',
  COMPLETED: '已完成'
}

function formatSla(status?: SlaStatus | null) {
  return status ? slaLabel[status] : '未设置'
}

function formatRemaining(minutes?: number | null, status?: SlaStatus | null) {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'OVERDUE') return '已超时'
  if (minutes == null) return '未设置截止时间'
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return hours > 0 ? `剩余 ${hours} 小时 ${rest} 分钟` : `剩余 ${rest} 分钟`
}
```

Add recommendation loader:

```ts
async function loadRecommendation() {
  if (!canAssignTicket.value) {
    recommendation.value = null
    return
  }

  loadingRecommendation.value = true
  try {
    recommendation.value = await getAssignmentRecommendation(ticketId.value)
  } finally {
    loadingRecommendation.value = false
  }
}

function useRecommendedAssignee() {
  if (recommendation.value?.recommendedAssigneeId) {
    selectedAssigneeId.value = String(recommendation.value.recommendedAssigneeId)
  }
}
```

After `await loadTicket()` in `onMounted`, call:

```ts
await loadRecommendation()
```

After successful assignment, set:

```ts
recommendation.value = null
```

- [ ] **Step 4: Implement detail template**

Above the assignment select:

```vue
<div v-if="canAssignTicket" class="ticket-recommendation-panel">
  <strong>智能推荐</strong>
  <p>{{ loadingRecommendation ? '正在计算推荐坐席' : recommendation?.reason || '暂无可推荐坐席' }}</p>
  <button
    v-if="recommendation?.recommendedAssigneeId"
    data-testid="use-recommended-assignee"
    type="button"
    @click="useRecommendedAssignee"
  >
    使用推荐坐席
  </button>
</div>
```

Add a side panel before flow logs:

```vue
<section class="ticket-detail-panel">
  <div class="panel-heading">
    <span>SLA 状态</span>
    <strong>{{ formatSla(ticket.slaStatus) }}</strong>
  </div>
  <dl class="ticket-ai-context">
    <div>
      <dt>截止时间</dt>
      <dd>{{ formatDate(ticket.deadlineAt) }}</dd>
    </div>
    <div>
      <dt>剩余时间</dt>
      <dd>{{ formatRemaining(ticket.slaRemainingMinutes, ticket.slaStatus) }}</dd>
    </div>
    <div>
      <dt>优先级规则</dt>
      <dd>{{ formatPriority(ticket.priority) }} 优先级按预设 SLA 时限计算</dd>
    </div>
  </dl>
</section>
```

- [ ] **Step 5: Run focused test and verify GREEN**

Run command from Step 2.

Expected: PASS.

- [ ] **Step 6: Update plan checkbox**

Mark Task 6 `[x] ⭐`.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/tickets/TicketDetailView.vue frontend/src/views/tickets/TicketDetailView.spec.ts \
  docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "feat: show assignment recommendation in ticket detail"
```

---

### Task 7: Full Verification and Browser Acceptance

**Files:**
- Modify: `docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md`

- [ ] **Step 1: Run backend tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn test
```

Expected: `Tests run` with `Failures: 0, Errors: 0`.

- [ ] **Step 2: Run frontend tests**

```bash
cd frontend
npm run test
```

Expected: all frontend tests pass.

- [ ] **Step 3: Run frontend build**

```bash
cd frontend
npm run build
```

Expected: Vite build succeeds.

- [ ] **Step 4: Run acceptance evidence**

```bash
cd /Users/xianghuaifeng/Documents/毕业设计/.worktrees/recovered-phase31
FRONTEND_BASE_URL=http://127.0.0.1:5175 \
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
tools/smoke/phase31-acceptance-evidence.sh
```

Expected: report has PASS for backend smoke, frontend dev smoke, frontend tests, frontend build, backend documentation coverage.

- [ ] **Step 5: Browser acceptance**

Using Chrome/in-app browser:

1. Login as `admin / Admin_123456`.
2. Open `/app/tickets/manage`.
3. Open a `PENDING_ASSIGN` ticket detail.
4. Confirm `智能推荐` block appears.
5. Click `使用推荐坐席`.
6. Confirm assignee dropdown changes to the recommended agent.
7. Confirm `SLA 状态` panel appears with deadline and status.
8. Assign the ticket using existing `分配给坐席`.
9. Open `/app/tickets/manage` again and confirm SLA chip appears in the list.
10. Login as `agent / Admin_123456` and open `/app/tickets/assigned`.
11. Confirm SLA chip appears for assigned tickets.

- [ ] **Step 6: Update plan checkbox**

Mark Task 7 `[x] ⭐`.

- [ ] **Step 7: Commit verification plan update**

```bash
git add docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md
git commit -m "test: verify assignment recommendation and sla"
```

---

## Final Checks

After all tasks:

```bash
git status --short
git log --oneline -8
```

Expected:

- Working tree clean.
- Commits are separated by concern:
  - SLA policy
  - SLA response exposure
  - recommendation backend
  - recommendation API
  - frontend SLA list
  - frontend recommendation detail
  - verification update

Do not push until the browser acceptance check has been completed and the user approves pushing the Phase 39/40 branch state.
