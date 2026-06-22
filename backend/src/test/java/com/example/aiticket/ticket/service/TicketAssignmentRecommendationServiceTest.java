package com.example.aiticket.ticket.service;

import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ticket.domain.Ticket;
import com.example.aiticket.ticket.domain.TicketCategory;
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

        @Override public Long nextTicketId() { return 0L; }
        @Override public Long nextFlowLogId() { return 0L; }
        @Override public Long nextCommentId() { return 0L; }
        @Override public Long nextTicketCategoryId() { return 0L; }
        @Override public AiSession findOwnedAiSession(Long sessionId, Long userId) { return null; }
        @Override public AiMessage findLatestAssistantMessage(Long sessionId, Long userId) { return null; }
        @Override public AiMessage findOwnedAssistantMessage(Long messageId, Long sessionId, Long userId) { return null; }
        @Override public int insertTicket(Long id, String ticketNo, String title, String description, TicketStatus status,
                                           TicketPriority priority, String aiPrioritySuggestion, Long categoryId,
                                           Long departmentId, Long creatorId, Long assigneeId, TicketSource source,
                                           Long sourceSessionId, Long sourceMessageId, String aiSummary,
                                           String aiSuggestion, String transferReason, LocalDateTime deadlineAt) { return 0; }
        @Override public int insertFlowLog(Long id, Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                                            TicketWorkflowAction action, Long operatorId, String operatorRole,
                                            String commentText) { return 0; }
        @Override public Ticket findTicketForUpdate(Long ticketId) { return null; }
        @Override public Ticket findCreatedTicket(Long ticketId, Long creatorId) { return null; }
        @Override public Ticket findAssignedTicket(Long ticketId, Long assigneeId) { return null; }
        @Override public Ticket findTicketById(Long ticketId) { return ticket; }
        @Override public List<AgentWorkload> listAgentWorkloads() { return workloads; }
        @Override public List<Ticket> listCreatedTickets(Long creatorId, int limit) { return List.of(); }
        @Override public List<Ticket> listAssignedTickets(Long assigneeId, int limit) { return List.of(); }
        @Override public List<Ticket> listManagedTickets(int limit) { return List.of(); }
        @Override public List<TicketFlowLog> listFlowLogs(Long ticketId) { return List.of(); }
        @Override public int insertComment(Long id, Long ticketId, Long authorId, TicketCommentType commentType,
                                            String content, boolean internal) { return 0; }
        @Override public List<TicketComment> listComments(Long ticketId, boolean includeInternal) { return List.of(); }
        @Override public int insertTicketCategory(Long id, String name, Long parentId, int sortOrder, boolean enabled) { return 0; }
        @Override public int updateTicketCategory(Long id, String name, Long parentId, int sortOrder) { return 0; }
        @Override public int updateTicketCategoryEnabled(Long id, boolean enabled) { return 0; }
        @Override public List<TicketCategory> listTicketCategories(boolean includeDisabled) { return List.of(); }
        @Override public int updateTicketStatus(Long ticketId, TicketStatus status, Long assigneeId,
                                                 LocalDateTime firstResolvedAt, LocalDateTime closedAt,
                                                 Integer incrementReopen) { return 0; }
    }
}
