CREATE TABLE document_chunk (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    parent_id UUID,
    chunk_type VARCHAR(16) NOT NULL,
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    heading_path VARCHAR(2000),
    text_content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    character_count INTEGER NOT NULL CHECK (character_count > 0),
    estimated_token_count INTEGER NOT NULL CHECK (estimated_token_count > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_chunk_parent
        FOREIGN KEY (parent_id) REFERENCES document_chunk (id) ON DELETE CASCADE,
    CONSTRAINT ck_document_chunk_type
        CHECK (chunk_type IN ('PARENT', 'CHILD')),
    CONSTRAINT uk_document_chunk_ordinal
        UNIQUE (document_id, chunk_type, ordinal)
);

CREATE INDEX idx_document_chunk_document_type_ordinal
    ON document_chunk (document_id, chunk_type, ordinal);

CREATE INDEX idx_document_chunk_content_hash
    ON document_chunk (content_hash);
