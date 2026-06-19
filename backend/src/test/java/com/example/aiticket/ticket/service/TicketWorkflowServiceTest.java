package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiMessageRole;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.domain.TicketWorkflowAction;
import com.example.aiticket.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketWorkflowServiceTest {
    @Test
    void createFromAiSessionInsertsTicketAndCreateFlowLog() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        Ticket ticket = service.createFromAiSession(7L, "USER", new CreateTicketFromAiSessionCommand(
                10L,
                null,
                "忘记密码需要人工处理",
                "用户验证码无法接收，需要人工核验身份。",
                1L,
                TicketPriority.NORMAL,
                "验证码无法接收"
        ));

        assertThat(ticket.id()).isEqualTo(100L);
        assertThat(ticket.ticketNo()).startsWith("TK");
        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_ASSIGN);
        assertThat(ticket.source()).isEqualTo(TicketSource.AI_SESSION);
        assertThat(ticket.sourceSessionId()).isEqualTo(10L);
        assertThat(ticket.sourceMessageId()).isEqualTo(21L);
        assertThat(mapper.insertedTickets).hasSize(1);
        assertThat(mapper.flowLogs).hasSize(1);
        assertThat(mapper.flowLogs.getFirst().action()).isEqualTo(TicketWorkflowAction.CREATE);
        assertThat(mapper.flowLogs.getFirst().toStatus()).isEqualTo(TicketStatus.PENDING_ASSIGN);
    }

    @Test
    void createFromAiSessionRejectsSessionNotOwnedByUser() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ownedSession = null;
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        assertThatThrownBy(() -> service.createFromAiSession(7L, "USER", new CreateTicketFromAiSessionCommand(
                99L, null, "标题", "描述", 1L, TicketPriority.NORMAL, "原因"
        )))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("AI session not found");

        assertThat(mapper.insertedTickets).isEmpty();
        assertThat(mapper.flowLogs).isEmpty();
    }

    @Test
    void assignPendingTicketMovesItToPendingProcessAndLogsAction() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        Ticket ticket = service.assign(2L, "ADMIN", 100L, 3L, "分配给演示坐席");

        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_PROCESS);
        assertThat(ticket.assigneeId()).isEqualTo(3L);
        assertThat(mapper.updatedStatus).isEqualTo(TicketStatus.PENDING_PROCESS);
        assertThat(mapper.updatedAssigneeId).isEqualTo(3L);
        assertThat(mapper.flowLogs.getFirst().fromStatus()).isEqualTo(TicketStatus.PENDING_ASSIGN);
        assertThat(mapper.flowLogs.getFirst().toStatus()).isEqualTo(TicketStatus.PENDING_PROCESS);
        assertThat(mapper.flowLogs.getFirst().action()).isEqualTo(TicketWorkflowAction.ASSIGN);
    }

    @Test
    void assignRejectsTicketThatIsNotPendingAssign() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PROCESSING, null);
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        assertThatThrownBy(() -> service.assign(2L, "ADMIN", 100L, 3L, "重复分配"))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("ticket status PROCESSING does not allow ASSIGN");

        assertThat(mapper.updatedStatus).isNull();
        assertThat(mapper.flowLogs).isEmpty();
    }

    private static final class FakeTicketMapper implements TicketMapper {
        private long nextTicketId = 100L;
        private long nextFlowLogId = 200L;
        private AiSession ownedSession = new AiSession(10L, 7L, "忘记密码", "忘记密码怎么处理？",
                true, LocalDateTime.now(), LocalDateTime.now());
        private AiMessage assistantMessage = new AiMessage(21L, 10L, 7L, AiMessageRole.ASSISTANT,
                "AI 建议转人工。", "mock-chat", false, 0.4, true,
                "验证码无法接收", LocalDateTime.now());
        private Ticket ticketForUpdate = ticketForUpdate(TicketStatus.PENDING_ASSIGN, null);
        private final List<Ticket> insertedTickets = new ArrayList<>();
        private final List<TestFlowLog> flowLogs = new ArrayList<>();
        private TicketStatus updatedStatus;
        private Long updatedAssigneeId;

        @Override
        public Long nextTicketId() {
            return nextTicketId++;
        }

        @Override
        public Long nextFlowLogId() {
            return nextFlowLogId++;
        }

        @Override
        public AiSession findOwnedAiSession(Long sessionId, Long userId) {
            if (ownedSession != null && ownedSession.id().equals(sessionId) && ownedSession.userId().equals(userId)) {
                return ownedSession;
            }
            return null;
        }

        @Override
        public AiMessage findLatestAssistantMessage(Long sessionId, Long userId) {
            if (assistantMessage != null && assistantMessage.sessionId().equals(sessionId)
                    && assistantMessage.userId().equals(userId)) {
                return assistantMessage;
            }
            return null;
        }

        @Override
        public AiMessage findOwnedAssistantMessage(Long messageId, Long sessionId, Long userId) {
            if (assistantMessage != null && assistantMessage.id().equals(messageId)
                    && assistantMessage.sessionId().equals(sessionId)
                    && assistantMessage.userId().equals(userId)) {
                return assistantMessage;
            }
            return null;
        }

        @Override
        public int insertTicket(Long id, String ticketNo, String title, String description, TicketStatus status,
                                TicketPriority priority, String aiPrioritySuggestion, Long categoryId,
                                Long departmentId, Long creatorId, Long assigneeId, TicketSource source,
                                Long sourceSessionId, Long sourceMessageId, String aiSummary, String aiSuggestion,
                                String transferReason, LocalDateTime deadlineAt) {
            insertedTickets.add(new Ticket(id, ticketNo, title, description, status, priority,
                    aiPrioritySuggestion, categoryId, departmentId, creatorId, assigneeId, source,
                    sourceSessionId, sourceMessageId, aiSummary, aiSuggestion, transferReason, deadlineAt,
                    null, null, 0, false, LocalDateTime.now(), LocalDateTime.now()));
            return 1;
        }

        @Override
        public int insertFlowLog(Long id, Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                                 TicketWorkflowAction action, Long operatorId, String operatorRole,
                                 String commentText) {
            flowLogs.add(new TestFlowLog(id, ticketId, fromStatus, toStatus, action, operatorId,
                    operatorRole, commentText));
            return 1;
        }

        @Override
        public Ticket findTicketForUpdate(Long ticketId) {
            return ticketForUpdate;
        }

        @Override
        public Ticket findCreatedTicket(Long ticketId, Long creatorId) {
            return null;
        }

        @Override
        public Ticket findAssignedTicket(Long ticketId, Long assigneeId) {
            return null;
        }

        @Override
        public Ticket findTicketById(Long ticketId) {
            return ticketForUpdate;
        }

        @Override
        public List<Ticket> listCreatedTickets(Long creatorId, int limit) {
            return List.of();
        }

        @Override
        public List<Ticket> listAssignedTickets(Long assigneeId, int limit) {
            return List.of();
        }

        @Override
        public List<Ticket> listManagedTickets(int limit) {
            return List.of();
        }

        @Override
        public int updateTicketStatus(Long ticketId, TicketStatus status, Long assigneeId,
                                      LocalDateTime firstResolvedAt, LocalDateTime closedAt,
                                      Integer incrementReopen) {
            this.updatedStatus = status;
            this.updatedAssigneeId = assigneeId;
            ticketForUpdate = ticketForUpdate(status, assigneeId);
            return 1;
        }

        private Ticket ticketForUpdate(TicketStatus status, Long assigneeId) {
            return new Ticket(100L, "TK100", "标题", "描述", status, TicketPriority.NORMAL,
                    null, 1L, null, 7L, assigneeId, TicketSource.AI_SESSION, 10L, 21L,
                    "摘要", "建议", "原因", null, null, null, 0, false,
                    LocalDateTime.now(), LocalDateTime.now());
        }
    }

    private record TestFlowLog(Long id, Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                               TicketWorkflowAction action, Long operatorId, String operatorRole,
                               String commentText) {
    }

}
