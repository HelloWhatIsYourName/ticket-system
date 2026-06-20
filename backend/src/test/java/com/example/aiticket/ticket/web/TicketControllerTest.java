package com.example.aiticket.ticket.web;

import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketComment;
import com.example.aiticket.ticket.domain.TicketCommentType;
import com.example.aiticket.ticket.domain.TicketPriority;
import com.example.aiticket.ticket.domain.TicketSource;
import com.example.aiticket.ticket.domain.TicketStatus;
import com.example.aiticket.ticket.service.AddTicketCommentCommand;
import com.example.aiticket.ticket.service.CreateTicketFromAiSessionCommand;
import com.example.aiticket.ticket.service.TicketWorkflowService;
import com.example.aiticket.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketControllerTest {
    @Test
    void endpointsKeepExpectedPermissions() throws Exception {
        assertThat(method("createFromAiSession", CreateTicketFromAiSessionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:create')");
        assertThat(method("myTickets", AuthenticatedUser.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:view:own')");
        assertThat(method("assignedTickets", AuthenticatedUser.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:process')");
        assertThat(method("managedTickets").getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:manage')");
        assertThat(method("detail", Long.class, AuthenticatedUser.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')");
        assertThat(method("addComment", Long.class, CreateTicketCommentRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')");
        assertThat(method("comments", Long.class, AuthenticatedUser.class).getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('ticket:view:own','ticket:process','ticket:manage')");
        assertThat(method("assign", Long.class, AssignTicketRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:assign')");
        assertThat(method("start", Long.class, TicketActionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:process')");
        assertThat(method("resolve", Long.class, TicketActionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:process')");
        assertThat(method("reopen", Long.class, TicketActionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:view:own')");
        assertThat(method("confirmClose", Long.class, TicketActionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:view:own')");
        assertThat(method("close", Long.class, TicketActionRequest.class, AuthenticatedUser.class)
                .getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('ticket:manage')");
    }

    @Test
    void createMapsRequestToServiceAndResponse() {
        FakeTicketWorkflowService service = new FakeTicketWorkflowService();
        TicketController controller = new TicketController(service);

        TicketResponse response = controller.createFromAiSession(new CreateTicketFromAiSessionRequest(
                10L, 21L, "忘记密码需要人工", "验证码无法接收", 1L,
                TicketPriority.NORMAL, "AI 建议转人工"
        ), user()).data();

        assertThat(service.lastCreateCommand.sessionId()).isEqualTo(10L);
        assertThat(service.lastCreateCommand.assistantMessageId()).isEqualTo(21L);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.ticketNo()).isEqualTo("TK100");
        assertThat(response.status()).isEqualTo(TicketStatus.PENDING_ASSIGN);
        assertThat(Arrays.stream(TicketResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("deleted");
    }

    @Test
    void listsAndDetailMapTickets() {
        TicketController controller = new TicketController(new FakeTicketWorkflowService());

        assertThat(controller.myTickets(user()).data()).hasSize(1);
        assertThat(controller.assignedTickets(agent()).data()).hasSize(1);
        assertThat(controller.managedTickets().data()).hasSize(1);
        assertThat(controller.detail(100L, user()).data().id()).isEqualTo(100L);
        assertThat(controller.detail(100L, user()).data().flowLogs()).hasSize(1);
    }

    @Test
    void commentEndpointsMapRequestAndResponse() {
        FakeTicketWorkflowService service = new FakeTicketWorkflowService();
        TicketController controller = new TicketController(service);

        TicketCommentResponse created = controller.addComment(100L, new CreateTicketCommentRequest(
                TicketCommentType.INTERNAL_NOTE, "身份信息已核验"
        ), agent()).data();
        List<TicketCommentResponse> comments = controller.comments(100L, agent()).data();

        assertThat(service.lastCommentCommand.commentType()).isEqualTo(TicketCommentType.INTERNAL_NOTE);
        assertThat(service.lastCommentCommand.content()).isEqualTo("身份信息已核验");
        assertThat(created.id()).isEqualTo(300L);
        assertThat(created.internal()).isTrue();
        assertThat(comments).hasSize(1);
        assertThat(Arrays.stream(TicketCommentResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .contains("commentType", "content", "internal")
                .doesNotContain("deleted");
    }

    @Test
    void actionEndpointsReturnUpdatedTicket() {
        TicketController controller = new TicketController(new FakeTicketWorkflowService());
        TicketActionRequest action = new TicketActionRequest("处理意见");

        assertThat(controller.assign(100L, new AssignTicketRequest(3L, "分配"), admin()).data().status())
                .isEqualTo(TicketStatus.PENDING_PROCESS);
        assertThat(controller.start(100L, action, agent()).data().status()).isEqualTo(TicketStatus.PROCESSING);
        assertThat(controller.resolve(100L, action, agent()).data().status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(controller.reopen(100L, action, user()).data().status()).isEqualTo(TicketStatus.PROCESSING);
        assertThat(controller.confirmClose(100L, action, user()).data().status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(controller.close(100L, action, admin()).data().status()).isEqualTo(TicketStatus.CLOSED);
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return TicketController.class.getMethod(name, parameterTypes);
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(7L, "user", "User", 1, List.of("USER"),
                List.of("ticket:create", "ticket:view:own"));
    }

    private AuthenticatedUser agent() {
        return new AuthenticatedUser(3L, "agent", "Agent", 1, List.of("AGENT"),
                List.of("ticket:view:own", "ticket:process"));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(2L, "admin", "Admin", 1, List.of("ADMIN"),
                List.of("ticket:assign", "ticket:manage"));
    }

    private static final class FakeTicketWorkflowService extends TicketWorkflowService {
        private CreateTicketFromAiSessionCommand lastCreateCommand;
        private AddTicketCommentCommand lastCommentCommand;

        private FakeTicketWorkflowService() {
            super(null);
        }

        @Override
        public Ticket createFromAiSession(Long userId, String operatorRole, CreateTicketFromAiSessionCommand command) {
            this.lastCreateCommand = command;
            return ticket(TicketStatus.PENDING_ASSIGN, null);
        }

        @Override
        public List<Ticket> listCreatedTickets(Long creatorId, int limit) {
            return List.of(ticket(TicketStatus.PENDING_ASSIGN, null));
        }

        @Override
        public List<Ticket> listAssignedTickets(Long assigneeId, int limit) {
            return List.of(ticket(TicketStatus.PENDING_PROCESS, assigneeId));
        }

        @Override
        public List<Ticket> listManagedTickets(int limit) {
            return List.of(ticket(TicketStatus.PROCESSING, 3L));
        }

        @Override
        public Ticket getTicket(Long userId, Long ticketId, boolean canManage, boolean canProcess) {
            return ticket(TicketStatus.PENDING_ASSIGN, null);
        }

        @Override
        public com.example.aiticket.ticket.domain.TicketDetail getTicketDetail(Long userId, Long ticketId,
                                                                               boolean canManage, boolean canProcess) {
            return new com.example.aiticket.ticket.domain.TicketDetail(
                    ticket(TicketStatus.PENDING_ASSIGN, null),
                    List.of(new com.example.aiticket.ticket.domain.TicketFlowLog(
                            201L, 100L, null, TicketStatus.PENDING_ASSIGN,
                            com.example.aiticket.ticket.domain.TicketWorkflowAction.CREATE,
                            7L, "USER", "创建工单", LocalDateTime.now()))
            );
        }

        @Override
        public TicketComment addComment(Long userId, String operatorRole, Long ticketId, boolean canManage,
                                        boolean canProcess, AddTicketCommentCommand command) {
            this.lastCommentCommand = command;
            return comment(command.commentType());
        }

        @Override
        public List<TicketComment> listComments(Long userId, Long ticketId, boolean canManage, boolean canProcess) {
            return List.of(comment(TicketCommentType.INTERNAL_NOTE));
        }

        @Override
        public Ticket assign(Long operatorId, String operatorRole, Long ticketId, Long assigneeId, String comment) {
            return ticket(TicketStatus.PENDING_PROCESS, assigneeId);
        }

        @Override
        public Ticket startProcessing(Long operatorId, String operatorRole, Long ticketId, String comment) {
            return ticket(TicketStatus.PROCESSING, operatorId);
        }

        @Override
        public Ticket resolve(Long operatorId, String operatorRole, Long ticketId, String comment) {
            return ticket(TicketStatus.RESOLVED, operatorId);
        }

        @Override
        public Ticket reopen(Long operatorId, String operatorRole, Long ticketId, String comment) {
            return ticket(TicketStatus.PROCESSING, 3L);
        }

        @Override
        public Ticket confirmClose(Long operatorId, String operatorRole, Long ticketId, String comment) {
            return ticket(TicketStatus.CLOSED, 3L);
        }

        @Override
        public Ticket close(Long operatorId, String operatorRole, Long ticketId, String comment) {
            return ticket(TicketStatus.CLOSED, 3L);
        }

        private Ticket ticket(TicketStatus status, Long assigneeId) {
            return new Ticket(100L, "TK100", "忘记密码需要人工", "验证码无法接收", status,
                    TicketPriority.NORMAL, null, 1L, null, 7L, assigneeId, TicketSource.AI_SESSION,
                    10L, 21L, "忘记密码需要人工", "AI 建议转人工", "AI 建议转人工",
                    null, null, null, 0, false, LocalDateTime.now(), LocalDateTime.now());
        }

        private TicketComment comment(TicketCommentType type) {
            return new TicketComment(300L, 100L, 3L, type, "身份信息已核验",
                    type == TicketCommentType.INTERNAL_NOTE, LocalDateTime.now());
        }
    }
}
