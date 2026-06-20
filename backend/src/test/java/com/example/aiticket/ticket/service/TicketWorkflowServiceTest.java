package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiMessageRole;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketComment;
import com.example.aiticket.ticket.domain.TicketCommentType;
import com.example.aiticket.ticket.domain.TicketFlowLog;
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

    @Test
    void agentCanStartAndResolveAssignedTicket() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PENDING_PROCESS, 3L);
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        Ticket processing = service.startProcessing(3L, "AGENT", 100L, "开始处理");
        assertThat(processing.status()).isEqualTo(TicketStatus.PROCESSING);
        assertThat(mapper.flowLogs.getFirst().action()).isEqualTo(TicketWorkflowAction.START_PROCESS);

        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PROCESSING, 3L);
        Ticket resolved = service.resolve(3L, "AGENT", 100L, "已协助用户重置密码");
        assertThat(resolved.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(mapper.updatedFirstResolvedAt).isNotNull();
        assertThat(mapper.flowLogs.get(1).action()).isEqualTo(TicketWorkflowAction.RESOLVE);
    }

    @Test
    void userCanReopenAndConfirmResolvedOwnTicket() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.RESOLVED, 3L);
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        Ticket reopened = service.reopen(7L, "USER", 100L, "问题仍未解决");
        assertThat(reopened.status()).isEqualTo(TicketStatus.PROCESSING);
        assertThat(reopened.reopenCount()).isEqualTo(1);
        assertThat(mapper.updatedIncrementReopen).isEqualTo(1);
        assertThat(mapper.flowLogs.getFirst().action()).isEqualTo(TicketWorkflowAction.REOPEN);

        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.RESOLVED, 3L);
        Ticket closed = service.confirmClose(7L, "USER", 100L, "确认解决");
        assertThat(closed.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(mapper.updatedClosedAt).isNotNull();
        assertThat(mapper.flowLogs.get(1).action()).isEqualTo(TicketWorkflowAction.CONFIRM_CLOSE);
    }

    @Test
    void adminCanCloseProcessingTicket() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PROCESSING, 3L);
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        Ticket closed = service.close(2L, "ADMIN", 100L, "无效工单");

        assertThat(closed.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(mapper.updatedClosedAt).isNotNull();
        assertThat(mapper.flowLogs.getFirst().action()).isEqualTo(TicketWorkflowAction.CLOSE);
    }

    @Test
    void listAndDetailHelpersUseCorrectMapperScopes() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.flowLogs.add(new TestFlowLog(201L, 100L, TicketStatus.PENDING_ASSIGN,
                TicketStatus.PENDING_PROCESS, TicketWorkflowAction.ASSIGN, 2L, "ADMIN", "分配"));
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        assertThat(service.listCreatedTickets(7L, 100)).hasSize(1);
        assertThat(service.listAssignedTickets(3L, 100)).hasSize(1);
        assertThat(service.listManagedTickets(100)).hasSize(1);
        assertThat(service.getTicket(7L, 100L, false, false).creatorId()).isEqualTo(7L);
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PENDING_PROCESS, 3L);
        assertThat(service.getTicket(3L, 100L, false, true).assigneeId()).isEqualTo(3L);
        assertThat(service.getTicketDetail(2L, 100L, true, false).ticket().id()).isEqualTo(100L);
        assertThat(service.getTicketDetail(2L, 100L, true, false).flowLogs()).hasSize(1);
    }

    @Test
    void creatorCanAddUserReplyAndOnlySeesExternalComments() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.comments.add(new TestComment(301L, 100L, 3L, TicketCommentType.INTERNAL_NOTE, "内部排查备注", true));
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        TicketComment comment = service.addComment(7L, "USER", 100L, false, false,
                new AddTicketCommentCommand(TicketCommentType.USER_REPLY, "我补充一下手机号后四位。"));

        assertThat(comment.id()).isEqualTo(300L);
        assertThat(comment.commentType()).isEqualTo(TicketCommentType.USER_REPLY);
        assertThat(comment.internal()).isFalse();
        assertThat(mapper.comments).hasSize(2);
        assertThat(service.listComments(7L, 100L, false, false))
                .extracting(TicketComment::commentType)
                .containsExactly(TicketCommentType.USER_REPLY);
    }

    @Test
    void assignedAgentCanAddReplyAndInternalNoteAndSeesInternalComments() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        mapper.ticketForUpdate = mapper.ticketForUpdate(TicketStatus.PROCESSING, 3L);
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        TicketComment reply = service.addComment(3L, "AGENT", 100L, false, true,
                new AddTicketCommentCommand(TicketCommentType.AGENT_REPLY, "已经帮你提交人工核验。"));
        TicketComment note = service.addComment(3L, "AGENT", 100L, false, true,
                new AddTicketCommentCommand(TicketCommentType.INTERNAL_NOTE, "用户身份信息已核验。"));

        assertThat(reply.internal()).isFalse();
        assertThat(note.internal()).isTrue();
        assertThat(service.listComments(3L, 100L, false, true))
                .extracting(TicketComment::commentType)
                .containsExactly(TicketCommentType.AGENT_REPLY, TicketCommentType.INTERNAL_NOTE);
    }

    @Test
    void ordinaryCreatorCannotAddInternalNoteOrAgentReply() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketWorkflowService service = new TicketWorkflowService(mapper);

        assertThatThrownBy(() -> service.addComment(7L, "USER", 100L, false, false,
                new AddTicketCommentCommand(TicketCommentType.INTERNAL_NOTE, "普通用户不能写内部备注")))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("comment type INTERNAL_NOTE is not allowed");
        assertThatThrownBy(() -> service.addComment(7L, "USER", 100L, false, false,
                new AddTicketCommentCommand(TicketCommentType.AGENT_REPLY, "普通用户不能冒充坐席回复")))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("comment type AGENT_REPLY is not allowed");
    }

    private static final class FakeTicketMapper implements TicketMapper {
        private long nextTicketId = 100L;
        private long nextFlowLogId = 200L;
        private long nextCommentId = 300L;
        private AiSession ownedSession = new AiSession(10L, 7L, "忘记密码", "忘记密码怎么处理？",
                true, LocalDateTime.now(), LocalDateTime.now());
        private AiMessage assistantMessage = new AiMessage(21L, 10L, 7L, AiMessageRole.ASSISTANT,
                "AI 建议转人工。", "mock-chat", false, 0.4, true,
                "验证码无法接收", LocalDateTime.now());
        private Ticket ticketForUpdate = ticketForUpdate(TicketStatus.PENDING_ASSIGN, null);
        private final List<Ticket> insertedTickets = new ArrayList<>();
        private final List<TestFlowLog> flowLogs = new ArrayList<>();
        private final List<TestComment> comments = new ArrayList<>();
        private TicketStatus updatedStatus;
        private Long updatedAssigneeId;
        private LocalDateTime updatedFirstResolvedAt;
        private LocalDateTime updatedClosedAt;
        private Integer updatedIncrementReopen;

        @Override
        public Long nextTicketId() {
            return nextTicketId++;
        }

        @Override
        public Long nextFlowLogId() {
            return nextFlowLogId++;
        }

        @Override
        public Long nextCommentId() {
            return nextCommentId++;
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
            if (ticketForUpdate.id().equals(ticketId) && ticketForUpdate.creatorId().equals(creatorId)) {
                return ticketForUpdate;
            }
            return null;
        }

        @Override
        public Ticket findAssignedTicket(Long ticketId, Long assigneeId) {
            if (ticketForUpdate.id().equals(ticketId) && assigneeId.equals(ticketForUpdate.assigneeId())) {
                return ticketForUpdate;
            }
            return null;
        }

        @Override
        public Ticket findTicketById(Long ticketId) {
            return ticketForUpdate;
        }

        @Override
        public List<Ticket> listCreatedTickets(Long creatorId, int limit) {
            return List.of(ticketForUpdate);
        }

        @Override
        public List<Ticket> listAssignedTickets(Long assigneeId, int limit) {
            return List.of(ticketForUpdate(TicketStatus.PENDING_PROCESS, assigneeId));
        }

        @Override
        public List<Ticket> listManagedTickets(int limit) {
            return List.of(ticketForUpdate);
        }

        @Override
        public List<TicketFlowLog> listFlowLogs(Long ticketId) {
            return flowLogs.stream()
                    .filter(flowLog -> flowLog.ticketId().equals(ticketId))
                    .map(flowLog -> new TicketFlowLog(flowLog.id(), flowLog.ticketId(), flowLog.fromStatus(),
                            flowLog.toStatus(), flowLog.action(), flowLog.operatorId(), flowLog.operatorRole(),
                            flowLog.commentText(), LocalDateTime.now()))
                    .toList();
        }

        @Override
        public int insertComment(Long id, Long ticketId, Long authorId, TicketCommentType commentType,
                                 String content, boolean internal) {
            comments.add(new TestComment(id, ticketId, authorId, commentType, content, internal));
            return 1;
        }

        @Override
        public List<TicketComment> listComments(Long ticketId, boolean includeInternal) {
            return comments.stream()
                    .filter(comment -> comment.ticketId().equals(ticketId))
                    .filter(comment -> includeInternal || !comment.internal())
                    .map(comment -> new TicketComment(comment.id(), comment.ticketId(), comment.authorId(),
                            comment.commentType(), comment.content(), comment.internal(), LocalDateTime.now()))
                    .toList();
        }

        @Override
        public int updateTicketStatus(Long ticketId, TicketStatus status, Long assigneeId,
                                      LocalDateTime firstResolvedAt, LocalDateTime closedAt,
                                      Integer incrementReopen) {
            this.updatedStatus = status;
            this.updatedAssigneeId = assigneeId;
            this.updatedFirstResolvedAt = firstResolvedAt;
            this.updatedClosedAt = closedAt;
            this.updatedIncrementReopen = incrementReopen;
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

    private record TestComment(Long id, Long ticketId, Long authorId, TicketCommentType commentType,
                               String content, boolean internal) {
    }

}
