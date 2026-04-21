CREATE TABLE IF NOT EXISTS event_templates (
    event_template_id varchar(36) PRIMARY KEY,
    company_id varchar(36) NOT NULL,
    name varchar(255) NOT NULL,
    content text NOT NULL,
    description text,
    status varchar(30) DEFAULT 'ACTIVE',
    created_by varchar(36),
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_event_templates_company
    ON event_templates(company_id);

CREATE TABLE IF NOT EXISTS event_instances (
    event_instance_id varchar(36) PRIMARY KEY,
    company_id varchar(36) NOT NULL,
    event_template_id varchar(36) NOT NULL,
    event_at timestamptz NOT NULL,
    source_type varchar(30) NOT NULL,
    source_department_ids jsonb,
    source_user_ids jsonb,
    participant_user_ids jsonb NOT NULL,
    status varchar(30) DEFAULT 'PUBLISHED',
    notified_at timestamptz,
    created_by varchar(36),
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_event_instances_company
    ON event_instances(company_id);

CREATE INDEX IF NOT EXISTS ix_event_instances_template
    ON event_instances(company_id, event_template_id);

CREATE INDEX IF NOT EXISTS ix_event_instances_date
    ON event_instances(company_id, event_at);
