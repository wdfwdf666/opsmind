CREATE TABLE agent_run (
    id UUID PRIMARY KEY,
    query_text TEXT NOT NULL,
    document_id UUID,
    status VARCHAR(32) NOT NULL,
    answer_text TEXT,
    refused BOOLEAN,
    refusal_reason VARCHAR(64),
    top_evidence_score DOUBLE PRECISION,
    citations_json TEXT NOT NULL DEFAULT '[]',
    error_code VARCHAR(128),
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_agent_run_status_created_at ON agent_run (status, created_at);

CREATE TABLE agent_step (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    sequence_no INTEGER NOT NULL CHECK (sequence_no > 0),
    step_type VARCHAR(32) NOT NULL,
    tool_name VARCHAR(128),
    tool_risk VARCHAR(32),
    success BOOLEAN NOT NULL,
    detail_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES agent_run (id) ON DELETE CASCADE,
    CONSTRAINT uk_agent_step_run_sequence UNIQUE (run_id, sequence_no)
);

CREATE INDEX idx_agent_step_run_sequence ON agent_step (run_id, sequence_no);
