CREATE TABLE chunk_embedding (
    chunk_id UUID NOT NULL,
    document_id UUID NOT NULL,
    embedding_model VARCHAR(200) NOT NULL,
    dimension INTEGER NOT NULL CHECK (dimension > 0),
    vector_data TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (chunk_id, embedding_model),
    CONSTRAINT fk_chunk_embedding_chunk
        FOREIGN KEY (chunk_id) REFERENCES document_chunk (id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embedding_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE
);

CREATE INDEX idx_chunk_embedding_document_model
    ON chunk_embedding (document_id, embedding_model);

CREATE INDEX idx_chunk_embedding_model
    ON chunk_embedding (embedding_model);
