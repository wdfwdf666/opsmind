CREATE TABLE knowledge_document (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(512) NOT NULL,
    media_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_knowledge_document_sha256 UNIQUE (sha256)
);

CREATE INDEX idx_knowledge_document_status_created_at
    ON knowledge_document (status, created_at);

CREATE TABLE ingestion_task (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_message VARCHAR(2000),
    CONSTRAINT fk_ingestion_task_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE,
    CONSTRAINT uk_ingestion_task_document UNIQUE (document_id)
);

CREATE INDEX idx_ingestion_task_status_updated_at
    ON ingestion_task (status, updated_at);

