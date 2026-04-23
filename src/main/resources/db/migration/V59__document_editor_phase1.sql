-- Rich-text / editor documents (Phase 1): draft + publish snapshots, version history, activity

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS content_kind varchar(30) NOT NULL DEFAULT 'FILE',
    ADD COLUMN IF NOT EXISTS draft_json text,
    ADD COLUMN IF NOT EXISTS published_json text,
    ADD COLUMN IF NOT EXISTS published_at timestamptz,
    ADD COLUMN IF NOT EXISTS published_by varchar(36);

COMMENT ON COLUMN documents.content_kind IS 'FILE = uploaded binary doc; EDITOR = in-app rich text JSON';

CREATE INDEX IF NOT EXISTS ix_documents_company_content_kind
    ON documents (company_id, content_kind);

ALTER TABLE document_versions
    ADD COLUMN IF NOT EXISTS content_json text;

COMMENT ON COLUMN document_versions.content_json IS 'Published rich-text JSON snapshot; null for file-only versions';

CREATE TABLE IF NOT EXISTS document_activity_logs (
    document_activity_log_id varchar(36) PRIMARY KEY,
    company_id               varchar(36),
    document_id              varchar(36),
    action                   varchar(50)  NOT NULL,
    actor_user_id            varchar(36),
    detail_json              text,
    created_at               timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_activity_logs_company_doc
    ON document_activity_logs (company_id, document_id, created_at DESC);

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604240059000001', NULL, 'com.sme.document.createDraft', 'Create rich-text document draft', 'ACTIVE'),
('202604240059000002', NULL, 'com.sme.document.updateDraft', 'Update rich-text document draft', 'ACTIVE'),
('202604240059000003', NULL, 'com.sme.document.autosave', 'Autosave rich-text document draft', 'ACTIVE'),
('202604240059000004', NULL, 'com.sme.document.publish', 'Publish rich-text document version', 'ACTIVE'),
('202604240059000005', NULL, 'com.sme.document.detail', 'Get rich-text document detail', 'ACTIVE'),
('202604240059000006', NULL, 'com.sme.document.list', 'List rich-text documents for tenant', 'ACTIVE'),
('202604240059000007', NULL, 'com.sme.document.version.list', 'List document versions', 'ACTIVE'),
('202604240059000008', NULL, 'com.sme.document.version.get', 'Get one document version', 'ACTIVE'),
('202604240059000009', NULL, 'com.sme.document.read.mark', 'Mark document read (collaboration)', 'ACTIVE'),
('202604240059000010', NULL, 'com.sme.document.read.list', 'List document read receipts', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
