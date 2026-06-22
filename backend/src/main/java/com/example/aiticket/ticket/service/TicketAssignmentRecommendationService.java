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
                .min(Comparator.comparing(AgentWorkload::activeTicketCount)
                        .thenComparing(AgentWorkload::userId))
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
