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

class TicketCategoryServiceTest {
    @Test
    void createUsesDefaultsAndPersistsCategory() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketCategoryService service = new TicketCategoryService(mapper);

        TicketCategory category = service.create("  售后问题  ", null, null, null);

        assertThat(category.id()).isEqualTo(100L);
        assertThat(category.name()).isEqualTo("售后问题");
        assertThat(category.sortOrder()).isZero();
        assertThat(category.enabled()).isTrue();
        assertThat(mapper.categories).extracting(TicketCategory::name).contains("售后问题");
    }

    @Test
    void updateRenamesAndReordersCategory() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketCategoryService service = new TicketCategoryService(mapper);

        TicketCategory category = service.update(1L, "账号问题", null, 8);

        assertThat(category.id()).isEqualTo(1L);
        assertThat(category.name()).isEqualTo("账号问题");
        assertThat(category.sortOrder()).isEqualTo(8);
        assertThat(mapper.updatedCategoryId).isEqualTo(1L);
    }

    @Test
    void enableDisableAndListUseMapper() {
        FakeTicketMapper mapper = new FakeTicketMapper();
        TicketCategoryService service = new TicketCategoryService(mapper);

        service.disable(1L);
        assertThat(mapper.updatedEnabled).isFalse();

        service.enable(1L);
        assertThat(mapper.updatedEnabled).isTrue();

        assertThat(service.list(false)).hasSize(1);
        assertThat(mapper.lastIncludeDisabled).isFalse();
    }

    @Test
    void blankNameIsRejected() {
        TicketCategoryService service = new TicketCategoryService(new FakeTicketMapper());

        assertThatThrownBy(() -> service.create(" ", null, null, null))
                .isInstanceOf(TicketWorkflowException.class)
                .hasMessage("ticket category name is required");
    }

    private static final class FakeTicketMapper implements TicketMapper {
        private long nextTicketCategoryId = 100L;
        private final List<TicketCategory> categories = new ArrayList<>(
                List.of(new TicketCategory(1L, "通用问题", null, 1, true, LocalDateTime.now(), LocalDateTime.now()))
        );
        private Long updatedCategoryId;
        private Boolean updatedEnabled;
        private Boolean lastIncludeDisabled;

        @Override
        public Long nextTicketId() {
            return 1L;
        }

        @Override
        public Long nextFlowLogId() {
            return 1L;
        }

        @Override
        public Long nextCommentId() {
            return 1L;
        }

        @Override
        public Long nextTicketCategoryId() {
            return nextTicketCategoryId++;
        }

        @Override
        public int insertTicketCategory(Long id, String name, Long parentId, int sortOrder, boolean enabled) {
            categories.add(new TicketCategory(id, name, parentId, sortOrder, enabled, LocalDateTime.now(), LocalDateTime.now()));
            return 1;
        }

        @Override
        public int updateTicketCategory(Long id, String name, Long parentId, int sortOrder) {
            updatedCategoryId = id;
            categories.set(0, new TicketCategory(id, name, parentId, sortOrder, true, LocalDateTime.now(), LocalDateTime.now()));
            return 1;
        }

        @Override
        public int updateTicketCategoryEnabled(Long id, boolean enabled) {
            updatedCategoryId = id;
            updatedEnabled = enabled;
            return 1;
        }

        @Override
        public List<TicketCategory> listTicketCategories(boolean includeDisabled) {
            lastIncludeDisabled = includeDisabled;
            return categories;
        }

        @Override
        public AiSession findOwnedAiSession(Long sessionId, Long userId) {
            return null;
        }

        @Override
        public AiMessage findLatestAssistantMessage(Long sessionId, Long userId) {
            return null;
        }

        @Override
        public AiMessage findOwnedAssistantMessage(Long messageId, Long sessionId, Long userId) {
            return null;
        }

        @Override
        public int insertTicket(Long id, String ticketNo, String title, String description, TicketStatus status,
                                TicketPriority priority, String aiPrioritySuggestion, Long categoryId, Long departmentId,
                                Long creatorId, Long assigneeId, TicketSource source, Long sourceSessionId,
                                Long sourceMessageId, String aiSummary, String aiSuggestion, String transferReason,
                                LocalDateTime deadlineAt) {
            return 0;
        }

        @Override
        public int insertFlowLog(Long id, Long ticketId, TicketStatus fromStatus, TicketStatus toStatus,
                                 TicketWorkflowAction action, Long operatorId, String operatorRole, String commentText) {
            return 0;
        }

        @Override
        public Ticket findTicketForUpdate(Long ticketId) {
            return null;
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
            return null;
        }

        @Override
        public List<AgentWorkload> listAgentWorkloads() {
            return List.of();
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
        public List<TicketFlowLog> listFlowLogs(Long ticketId) {
            return List.of();
        }

        @Override
        public int insertComment(Long id, Long ticketId, Long authorId, TicketCommentType commentType,
                                 String content, boolean internal) {
            return 0;
        }

        @Override
        public List<TicketComment> listComments(Long ticketId, boolean includeInternal) {
            return List.of();
        }

        @Override
        public int updateTicketStatus(Long ticketId, TicketStatus status, Long assigneeId,
                                      LocalDateTime firstResolvedAt, LocalDateTime closedAt,
                                      Integer incrementReopen) {
            return 0;
        }
    }
}
