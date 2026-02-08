-- =========================
-- document_chunks (RAG: text chunks from uploaded documents)
-- =========================
CREATE TABLE IF NOT EXISTS document_chunks (
    chunk_id    varchar(36) PRIMARY KEY,
    company_id  varchar(36) NOT NULL,
    document_id varchar(36) NOT NULL,
    version_no  int DEFAULT 1,
    chunk_no    int NOT NULL,
    chunk_text  text NOT NULL,
    created_at  timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_chunks_company_document
    ON document_chunks(company_id, document_id);

CREATE INDEX IF NOT EXISTS ix_document_chunks_company
    ON document_chunks(company_id);

-- Optional: GIN full-text index for future semantic/search use
CREATE INDEX IF NOT EXISTS ix_document_chunks_chunk_text_gin
    ON document_chunks USING gin(to_tsvector('simple', chunk_text));
