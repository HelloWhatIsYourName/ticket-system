CREATE SEQUENCE ticket_category_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE ticket_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE ticket_flow_log_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE ticket_comment_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE ticket_category (
    id NUMBER(19) DEFAULT ticket_category_seq.NEXTVAL PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    parent_id NUMBER(19),
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    enabled NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ticket_category_parent FOREIGN KEY (parent_id) REFERENCES ticket_category(id),
    CONSTRAINT ck_ticket_category_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE ticket (
    id NUMBER(19) DEFAULT ticket_seq.NEXTVAL PRIMARY KEY,
    ticket_no VARCHAR2(64) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    description CLOB NOT NULL,
    status VARCHAR2(32) NOT NULL,
    priority VARCHAR2(32) NOT NULL,
    ai_priority_suggestion VARCHAR2(32),
    category_id NUMBER(19),
    department_id NUMBER(19),
    creator_id NUMBER(19) NOT NULL,
    assignee_id NUMBER(19),
    source VARCHAR2(32) NOT NULL,
    source_session_id NUMBER(19),
    source_message_id NUMBER(19),
    ai_summary VARCHAR2(1000),
    ai_suggestion CLOB,
    transfer_reason VARCHAR2(500),
    deadline_at TIMESTAMP,
    first_resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    reopen_count NUMBER(10) DEFAULT 0 NOT NULL,
    deleted NUMBER(1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_ticket_no UNIQUE (ticket_no),
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES ticket_category(id),
    CONSTRAINT fk_ticket_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id),
    CONSTRAINT fk_ticket_source_session FOREIGN KEY (source_session_id) REFERENCES ai_session(id),
    CONSTRAINT fk_ticket_source_message FOREIGN KEY (source_message_id) REFERENCES ai_message(id),
    CONSTRAINT ck_ticket_status CHECK (status IN ('PENDING_ASSIGN', 'PENDING_PROCESS', 'PROCESSING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_ticket_source CHECK (source IN ('MANUAL', 'AI_SESSION')),
    CONSTRAINT ck_ticket_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE ticket_flow_log (
    id NUMBER(19) DEFAULT ticket_flow_log_seq.NEXTVAL PRIMARY KEY,
    ticket_id NUMBER(19) NOT NULL,
    from_status VARCHAR2(32),
    to_status VARCHAR2(32) NOT NULL,
    action VARCHAR2(64) NOT NULL,
    operator_id NUMBER(19) NOT NULL,
    operator_role VARCHAR2(64),
    comment_text VARCHAR2(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ticket_flow_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_flow_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
    CONSTRAINT ck_ticket_flow_from_status CHECK (from_status IS NULL OR from_status IN ('PENDING_ASSIGN', 'PENDING_PROCESS', 'PROCESSING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_ticket_flow_to_status CHECK (to_status IN ('PENDING_ASSIGN', 'PENDING_PROCESS', 'PROCESSING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_ticket_flow_action CHECK (action IN ('CREATE', 'ASSIGN', 'START_PROCESS', 'RESOLVE', 'REOPEN', 'CONFIRM_CLOSE', 'CLOSE'))
);

CREATE TABLE ticket_comment (
    id NUMBER(19) DEFAULT ticket_comment_seq.NEXTVAL PRIMARY KEY,
    ticket_id NUMBER(19) NOT NULL,
    author_id NUMBER(19) NOT NULL,
    comment_type VARCHAR2(32) NOT NULL,
    content CLOB NOT NULL,
    internal NUMBER(1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ticket_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_comment_author FOREIGN KEY (author_id) REFERENCES sys_user(id),
    CONSTRAINT ck_ticket_comment_type CHECK (comment_type IN ('USER_REPLY', 'AGENT_REPLY', 'INTERNAL_NOTE', 'SYSTEM')),
    CONSTRAINT ck_ticket_comment_internal CHECK (internal IN (0, 1))
);

CREATE INDEX idx_ticket_creator_status ON ticket (creator_id, status, updated_at);
CREATE INDEX idx_ticket_assignee_status ON ticket (assignee_id, status, updated_at);
CREATE INDEX idx_ticket_source_session ON ticket (source_session_id);
CREATE INDEX idx_ticket_flow_ticket ON ticket_flow_log (ticket_id, created_at);
CREATE INDEX idx_ticket_comment_ticket ON ticket_comment (ticket_id, created_at);

INSERT INTO ticket_category (id, name, parent_id, sort_order, enabled)
VALUES (1, '通用问题', null, 1, 1);
