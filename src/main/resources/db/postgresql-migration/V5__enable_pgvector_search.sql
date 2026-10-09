CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE chunk_embedding
    ADD COLUMN vector_value vector;

UPDATE chunk_embedding
SET vector_value = ('[' || vector_data || ']')::vector
WHERE vector_value IS NULL;

ALTER TABLE chunk_embedding
    ALTER COLUMN vector_value SET NOT NULL;

CREATE INDEX idx_chunk_embedding_qwen_hnsw
    ON chunk_embedding
    USING hnsw ((vector_value::vector(1024)) vector_cosine_ops)
    WHERE embedding_model = 'qwen3.7-text-embedding' AND dimension = 1024;

CREATE INDEX idx_chunk_embedding_local_hnsw
    ON chunk_embedding
    USING hnsw ((vector_value::vector(256)) vector_cosine_ops)
    WHERE embedding_model = 'local-feature-hashing-v1' AND dimension = 256;
