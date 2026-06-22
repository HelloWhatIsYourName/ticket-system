package com.example.aiticket.ticket.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperXmlTest {
    @Test
    void ticketMigrationDefinesWorkflowTablesAndConstraints() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V5__ticket_workflow.sql"));

        assertThat(migration).contains("CREATE TABLE ticket_category");
        assertThat(migration).contains("CREATE TABLE ticket");
        assertThat(migration).contains("CREATE TABLE ticket_flow_log");
        assertThat(migration).contains("CREATE TABLE ticket_comment");
        assertThat(migration).contains("CONSTRAINT ck_ticket_status CHECK");
        assertThat(migration).contains("CONSTRAINT fk_ticket_source_session FOREIGN KEY (source_session_id) REFERENCES ai_session(id)");
        assertThat(migration).contains("CONSTRAINT fk_ticket_source_message FOREIGN KEY (source_message_id) REFERENCES ai_message(id)");
    }

    @Test
    void ticketMapperDeclaresCoreWorkflowStatements() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/TicketMapper.xml"));

        assertThat(mapper).contains("nextTicketId");
        assertThat(mapper).contains("insertTicket");
        assertThat(mapper).contains("insertFlowLog");
        assertThat(mapper).contains("findOwnedAiSession");
        assertThat(mapper).contains("findLatestAssistantMessage");
        assertThat(mapper).contains("findTicketForUpdate");
        assertThat(mapper).contains("updateTicketStatus");
        assertThat(mapper).contains("listFlowLogs");
        assertThat(mapper).contains("nextCommentId");
        assertThat(mapper).contains("insertComment");
        assertThat(mapper).contains("listComments");
        assertThat(mapper).contains("nextTicketCategoryId");
        assertThat(mapper).contains("insertTicketCategory");
        assertThat(mapper).contains("updateTicketCategory");
        assertThat(mapper).contains("updateTicketCategoryEnabled");
        assertThat(mapper).contains("listTicketCategories");
    }

    @Test
    void ticketMapperDeclaresAssignmentRecommendationWorkloadQuery() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/TicketMapper.xml"));

        assertThat(mapper).contains("listAgentWorkloads");
        assertThat(mapper).contains("r.role_code = 'AGENT'");
        assertThat(mapper).contains("t.status IN ('PENDING_PROCESS', 'PROCESSING', 'RESOLVED')");
        assertThat(mapper).contains("ORDER BY active_ticket_count ASC, u.id ASC");
    }
}
