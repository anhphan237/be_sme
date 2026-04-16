CREATE TABLE IF NOT EXISTS task_template_required_documents (
    task_template_required_document_id varchar(36) PRIMARY KEY,
    company_id varchar(36),
    task_template_id varchar(36),
    document_id varchar(36),
    created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_task_template_required_documents_company
    ON task_template_required_documents(company_id);

CREATE INDEX IF NOT EXISTS ix_task_template_required_documents_task_template
    ON task_template_required_documents(company_id, task_template_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_template_required_documents
    ON task_template_required_documents(company_id, task_template_id, document_id)
    WHERE company_id IS NOT NULL
      AND task_template_id IS NOT NULL
      AND document_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS task_required_documents (
    task_required_document_id varchar(36) PRIMARY KEY,
    company_id varchar(36),
    task_id varchar(36),
    document_id varchar(36),
    created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_task_required_documents_company
    ON task_required_documents(company_id);

CREATE INDEX IF NOT EXISTS ix_task_required_documents_task
    ON task_required_documents(company_id, task_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_required_documents
    ON task_required_documents(company_id, task_id, document_id)
    WHERE company_id IS NOT NULL
      AND task_id IS NOT NULL
      AND document_id IS NOT NULL;
