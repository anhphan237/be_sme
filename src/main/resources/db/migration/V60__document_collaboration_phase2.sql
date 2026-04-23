-- Phase 2: folders (tree) + threaded comments for documents; one folder placement per document per tenant

CREATE TABLE IF NOT EXISTS document_folders (
    folder_id        varchar(36) PRIMARY KEY,
    company_id       varchar(36),
    parent_folder_id varchar(36),
    name             varchar(255) NOT NULL,
    sort_order       int DEFAULT 0,
    status           varchar(30) DEFAULT 'ACTIVE',
    created_by       varchar(36),
    created_at       timestamptz DEFAULT now(),
    updated_at       timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_folders_company
    ON document_folders (company_id);

CREATE INDEX IF NOT EXISTS ix_document_folders_company_parent
    ON document_folders (company_id, parent_folder_id);

CREATE TABLE IF NOT EXISTS document_folder_items (
    document_folder_item_id varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    folder_id               varchar(36) NOT NULL,
    document_id             varchar(36) NOT NULL,
    created_at              timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_folder_items_company_document
    ON document_folder_items (company_id, document_id);

CREATE INDEX IF NOT EXISTS ix_document_folder_items_folder
    ON document_folder_items (company_id, folder_id);

CREATE TABLE IF NOT EXISTS document_comments (
    document_comment_id varchar(36) PRIMARY KEY,
    company_id          varchar(36),
    document_id         varchar(36) NOT NULL,
    parent_comment_id   varchar(36),
    author_user_id      varchar(36) NOT NULL,
    body                text NOT NULL,
    status              varchar(30) DEFAULT 'ACTIVE',
    created_at          timestamptz DEFAULT now(),
    updated_at          timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_comments_company_doc_created
    ON document_comments (company_id, document_id, created_at);

CREATE INDEX IF NOT EXISTS ix_document_comments_parent
    ON document_comments (company_id, parent_comment_id);

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604240060000001', NULL, 'com.sme.document.folder.create', 'Create document folder', 'ACTIVE'),
('202604240060000002', NULL, 'com.sme.document.folder.rename', 'Rename document folder', 'ACTIVE'),
('202604240060000003', NULL, 'com.sme.document.folder.move', 'Move document folder in tree', 'ACTIVE'),
('202604240060000004', NULL, 'com.sme.document.folder.list', 'List document folders', 'ACTIVE'),
('202604240060000005', NULL, 'com.sme.document.folder.addDocument', 'Place document in folder', 'ACTIVE'),
('202604240060000006', NULL, 'com.sme.document.folder.removeDocument', 'Remove document from folder', 'ACTIVE'),
('202604240060000007', NULL, 'com.sme.document.comment.add', 'Add document comment or reply', 'ACTIVE'),
('202604240060000008', NULL, 'com.sme.document.comment.list', 'List document comments', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
