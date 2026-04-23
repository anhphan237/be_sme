-- Phase 3: cross-document links, assignments, attachments (incl. video metadata via media_kind)

CREATE TABLE IF NOT EXISTS document_links (
    document_link_id      varchar(36) PRIMARY KEY,
    company_id            varchar(36),
    source_document_id    varchar(36) NOT NULL,
    target_document_id    varchar(36) NOT NULL,
    link_type             varchar(64) NOT NULL,
    status                varchar(30) DEFAULT 'ACTIVE',
    created_by            varchar(36),
    created_at            timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_links_company_src_tgt_type
    ON document_links (company_id, source_document_id, target_document_id, link_type)
    WHERE upper(coalesce(trim(status), '')) = 'ACTIVE';

CREATE INDEX IF NOT EXISTS ix_document_links_company_source
    ON document_links (company_id, source_document_id);

CREATE INDEX IF NOT EXISTS ix_document_links_company_target
    ON document_links (company_id, target_document_id);

CREATE TABLE IF NOT EXISTS document_assignments (
    document_assignment_id varchar(36) PRIMARY KEY,
    company_id               varchar(36),
    document_id              varchar(36) NOT NULL,
    assignee_user_id         varchar(36) NOT NULL,
    assigned_by_user_id      varchar(36),
    status                   varchar(30) DEFAULT 'ASSIGNED',
    assigned_at              timestamptz DEFAULT now(),
    updated_at               timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_assignments_active_assignee
    ON document_assignments (company_id, document_id, assignee_user_id)
    WHERE upper(coalesce(trim(status), '')) = 'ASSIGNED';

CREATE INDEX IF NOT EXISTS ix_document_assignments_company_doc
    ON document_assignments (company_id, document_id);

CREATE TABLE IF NOT EXISTS document_attachments (
    document_attachment_id varchar(36) PRIMARY KEY,
    company_id             varchar(36),
    document_id            varchar(36) NOT NULL,
    file_url               text NOT NULL,
    file_name              varchar(512),
    file_type              varchar(128),
    file_size_bytes        bigint,
    media_kind             varchar(30) NOT NULL DEFAULT 'FILE',
    status                 varchar(30) DEFAULT 'ACTIVE',
    uploaded_by            varchar(36),
    uploaded_at            timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_attachments_company_doc_uploaded
    ON document_attachments (company_id, document_id, uploaded_at DESC);

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604240061000001', NULL, 'com.sme.document.link.add', 'Add document link', 'ACTIVE'),
('202604240061000002', NULL, 'com.sme.document.link.remove', 'Remove document link', 'ACTIVE'),
('202604240061000003', NULL, 'com.sme.document.link.list', 'List document links', 'ACTIVE'),
('202604240061000004', NULL, 'com.sme.document.assignment.assign', 'Assign user to document', 'ACTIVE'),
('202604240061000005', NULL, 'com.sme.document.assignment.unassign', 'Unassign user from document', 'ACTIVE'),
('202604240061000006', NULL, 'com.sme.document.assignment.list', 'List document assignments', 'ACTIVE'),
('202604240061000007', NULL, 'com.sme.document.attachment.add', 'Add document attachment', 'ACTIVE'),
('202604240061000008', NULL, 'com.sme.document.attachment.remove', 'Remove document attachment', 'ACTIVE'),
('202604240061000009', NULL, 'com.sme.document.attachment.list', 'List document attachments', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
