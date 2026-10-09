CREATE TABLE document_content (
    document_id UUID PRIMARY KEY,
    title VARCHAR(1000),
    detected_media_type VARCHAR(128) NOT NULL,
    text_content TEXT NOT NULL,
    character_count INTEGER NOT NULL CHECK (character_count >= 0),
    parsed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_document_content_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE
);

CREATE INDEX idx_document_content_parsed_at
    ON document_content (parsed_at);
