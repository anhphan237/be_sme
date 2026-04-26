CREATE TABLE IF NOT EXISTS task_template_department_checkpoints (
    task_template_department_checkpoint_id varchar(36) PRIMARY KEY,
    company_id varchar(36) NOT NULL,
    task_template_id varchar(36) NOT NULL,
    department_id varchar(36) NOT NULL,
    require_evidence boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_task_template_department_checkpoints_company_task_department
    ON task_template_department_checkpoints (company_id, task_template_id, department_id);

CREATE INDEX IF NOT EXISTS ix_task_template_department_checkpoints_company_task
    ON task_template_department_checkpoints (company_id, task_template_id);

CREATE TABLE IF NOT EXISTS task_department_checkpoints (
    task_department_checkpoint_id varchar(36) PRIMARY KEY,
    company_id varchar(36) NOT NULL,
    task_id varchar(36) NOT NULL,
    department_id varchar(36) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    require_evidence boolean NOT NULL DEFAULT true,
    evidence_note text,
    evidence_ref text,
    confirmed_by varchar(36),
    confirmed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_task_department_checkpoints_company_task_department
    ON task_department_checkpoints (company_id, task_id, department_id);

CREATE INDEX IF NOT EXISTS ix_task_department_checkpoints_company_task
    ON task_department_checkpoints (company_id, task_id);

CREATE INDEX IF NOT EXISTS ix_task_department_checkpoints_company_task_status
    ON task_department_checkpoints (company_id, task_id, status);

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604260700000070037', NULL, 'com.sme.onboarding.task.department.confirm', 'Confirm onboarding task department checkpoint with evidence', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
