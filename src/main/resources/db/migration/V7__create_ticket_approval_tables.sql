ALTER TABLE agent_run
    ADD COLUMN task_type VARCHAR(32) NOT NULL DEFAULT 'KNOWLEDGE_QA';

CREATE TABLE agent_approval (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    tool_risk VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    draft_title VARCHAR(200) NOT NULL,
    draft_description TEXT NOT NULL,
    draft_priority VARCHAR(16) NOT NULL,
    decision_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_agent_approval_run FOREIGN KEY (run_id) REFERENCES agent_run (id) ON DELETE CASCADE,
    CONSTRAINT uk_agent_approval_run UNIQUE (run_id)
);

CREATE INDEX idx_agent_approval_status_created_at
    ON agent_approval (status, created_at);

CREATE TABLE support_ticket (
    id UUID PRIMARY KEY,
    agent_run_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_support_ticket_run FOREIGN KEY (agent_run_id) REFERENCES agent_run (id),
    CONSTRAINT uk_support_ticket_agent_run UNIQUE (agent_run_id)
);

CREATE INDEX idx_support_ticket_status_created_at
    ON support_ticket (status, created_at);
