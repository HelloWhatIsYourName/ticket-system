# Phase 27 Ticket Assignment Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the planned ticket assignment extensibility point into a real code boundary without changing V1 manual assignment behavior.

**Architecture:** Add a small `AssignmentStrategy` interface under the ticket service boundary, plus a `TicketAssignmentContext` record and a `ManualAssignmentStrategy` Spring component. `TicketWorkflowService.assign(...)` remains the public workflow entry point, but delegates assignee selection to the strategy before persisting the status transition.

**Tech Stack:** Java 21, Spring Boot 3, JUnit 5, AssertJ, Maven.

---

### Task 1: Lock Assignment Strategy Behavior With Tests

**Files:**
- Modify: `backend/src/test/java/com/example/aiticket/ticket/service/TicketWorkflowServiceTest.java`
- Create: `backend/src/test/java/com/example/aiticket/ticket/service/ManualAssignmentStrategyTest.java`

- [x] ⭐ **Step 1: Write the failing manual strategy test**

Add `ManualAssignmentStrategyTest` with:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualAssignmentStrategyTest {
    private final ManualAssignmentStrategy strategy = new ManualAssignmentStrategy();

    @Test
    void selectsRequestedAssigneeForManualAssignment() {
        TicketAssignmentContext context = new TicketAssignmentContext(ticket(), 2L, "ADMIN", 3L, "分配给演示坐席");

        assertThat(strategy.selectAssignee(context)).isEqualTo(3L);
    }

    @Test
    void rejectsMissingManualAssignee() {
        TicketAssignmentContext context = new TicketAssignmentContext(ticket(), 2L, "ADMIN", null, "缺少坐席");

        assertThatThrownBy(() -> strategy.selectAssignee(context))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("assignee is required");
    }

    private Ticket ticket() {
        return new Ticket(100L, "TK100", "标题", "描述", TicketStatus.PENDING_ASSIGN,
                TicketPriority.NORMAL, null, 1L, null, 7L, null, TicketSource.AI_SESSION,
                10L, 21L, "摘要", "建议", "原因", null, null, null, 0, false,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
```

- [x] ⭐ **Step 2: Write the failing workflow delegation test**

Update `TicketWorkflowServiceTest` so `assignPendingTicketMovesItToPendingProcessAndLogsAction` constructs:

```java
TicketWorkflowService service = new TicketWorkflowService(mapper, new ManualAssignmentStrategy());
```

Add a new test:

```java
@Test
void assignDelegatesAssigneeSelectionToStrategy() {
    FakeTicketMapper mapper = new FakeTicketMapper();
    RecordingAssignmentStrategy strategy = new RecordingAssignmentStrategy(4L);
    TicketWorkflowService service = new TicketWorkflowService(mapper, strategy);

    Ticket ticket = service.assign(2L, "ADMIN", 100L, 3L, "策略覆盖为坐席 4");

    assertThat(strategy.context.ticket().id()).isEqualTo(100L);
    assertThat(strategy.context.operatorId()).isEqualTo(2L);
    assertThat(strategy.context.operatorRole()).isEqualTo("ADMIN");
    assertThat(strategy.context.requestedAssigneeId()).isEqualTo(3L);
    assertThat(strategy.context.comment()).isEqualTo("策略覆盖为坐席 4");
    assertThat(ticket.assigneeId()).isEqualTo(4L);
    assertThat(mapper.updatedAssigneeId).isEqualTo(4L);
}
```

Also add this helper in the test class:

```java
private static final class RecordingAssignmentStrategy implements AssignmentStrategy {
    private final Long selectedAssigneeId;
    private TicketAssignmentContext context;

    private RecordingAssignmentStrategy(Long selectedAssigneeId) {
        this.selectedAssigneeId = selectedAssigneeId;
    }

    @Override
    public Long selectAssignee(TicketAssignmentContext context) {
        this.context = context;
        return selectedAssigneeId;
    }
}
```

- [x] ⭐ **Step 3: Run focused tests to verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=ManualAssignmentStrategyTest,TicketWorkflowServiceTest test
```

Actual: FAIL because `AssignmentStrategy`, `TicketAssignmentContext`, and `ManualAssignmentStrategy` did not exist.

### Task 2: Implement Assignment Strategy Boundary

**Files:**
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/AssignmentStrategy.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/TicketAssignmentContext.java`
- Create: `backend/src/main/java/com/example/aiticket/ticket/service/ManualAssignmentStrategy.java`
- Modify: `backend/src/main/java/com/example/aiticket/ticket/service/TicketWorkflowService.java`

- [x] ⭐ **Step 1: Add strategy interface and context**

Create:

```java
package com.example.aiticket.ticket.service;

public interface AssignmentStrategy {
    Long selectAssignee(TicketAssignmentContext context);
}
```

Create:

```java
package com.example.aiticket.ticket.service;

import com.example.aiticket.ticket.domain.Ticket;

public record TicketAssignmentContext(
        Ticket ticket,
        Long operatorId,
        String operatorRole,
        Long requestedAssigneeId,
        String comment
) {
}
```

- [x] ⭐ **Step 2: Add manual strategy implementation**

Create:

```java
package com.example.aiticket.ticket.service;

import org.springframework.stereotype.Component;

@Component
public class ManualAssignmentStrategy implements AssignmentStrategy {
    @Override
    public Long selectAssignee(TicketAssignmentContext context) {
        if (context.requestedAssigneeId() == null) {
            throw new TicketWorkflowException("assignee is required");
        }
        return context.requestedAssigneeId();
    }
}
```

- [x] ⭐ **Step 3: Wire strategy into workflow service**

Update `TicketWorkflowService`:

```java
private final TicketMapper mapper;
private final AssignmentStrategy assignmentStrategy;

public TicketWorkflowService(TicketMapper mapper, AssignmentStrategy assignmentStrategy) {
    this.mapper = mapper;
    this.assignmentStrategy = assignmentStrategy;
}
```

In `assign(...)`, remove the direct null check and call:

```java
Long selectedAssigneeId = assignmentStrategy.selectAssignee(
        new TicketAssignmentContext(current, operatorId, operatorRole, assigneeId, comment));
```

Use `selectedAssigneeId` in `mapper.updateTicketStatus(...)` and `copy(...)`.

- [x] ⭐ **Step 4: Run focused tests to verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=ManualAssignmentStrategyTest,TicketWorkflowServiceTest test
```

Actual: PASS, 14 tests, 0 failures.

### Task 3: Verification, Plan Marking, and Commit

**Files:**
- Modify: all Phase 27 files above.

- [x] ⭐ **Step 1: Run full backend tests**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Actual: PASS, 112 tests, 0 failures.

- [x] ⭐ **Step 2: Mark this plan complete**

Change each completed checkbox to `- [x] ⭐`.

- [x] ⭐ **Step 3: Inspect final diff**

Run:

```bash
git diff -- backend/src/main/java/com/example/aiticket/ticket/service backend/src/test/java/com/example/aiticket/ticket/service docs/superpowers/plans/2026-06-20-phase-27-ticket-assignment-strategy-implementation-plan.md
```

Expected: diff only contains the assignment strategy boundary, tests, and plan tracking.

- [x] ⭐ **Step 4: Commit**

Run:

```bash
git add backend/src/main/java/com/example/aiticket/ticket/service backend/src/test/java/com/example/aiticket/ticket/service docs/superpowers/plans/2026-06-20-phase-27-ticket-assignment-strategy-implementation-plan.md
git commit -m "feat: add ticket assignment strategy boundary"
```
