-- ==========================================
-- SME-Onboard Platform - PostgreSQL DDL (47 tables)
-- Convention:
--  - Each table has 1 PK ID (code-generated): <singular>_id
--  - No FK constraints
--  - company_id present in (almost) all tables, nullable for flexibility
--  - Minimal NOT NULL (only PK); timestamps default now()
--  - ID type: varchar(36) (String in code, UUID format string)
-- ==========================================

-- =========================
-- 1) companies
-- =========================
CREATE TABLE IF NOT EXISTS companies (
    company_id   varchar(36) PRIMARY KEY,
    name         varchar(255),
    tax_code     varchar(50),
    address      varchar(500),
    timezone     varchar(50),
    status       varchar(30) DEFAULT 'ACTIVE',
    created_at   timestamptz DEFAULT now(),
    updated_at   timestamptz DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_name
    ON companies(name)
    WHERE name IS NOT NULL;

-- =========================
-- 2) departments
-- =========================
CREATE TABLE IF NOT EXISTS departments (
    department_id varchar(36) PRIMARY KEY,
    company_id    varchar(36),
    name          varchar(255),
    type          varchar(30) DEFAULT 'OTHER', -- HR|IT|FIN|OPS|OTHER
    status        varchar(30) DEFAULT 'ACTIVE',
    created_at    timestamptz DEFAULT now(),
    updated_at    timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_departments_company
    ON departments(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_departments_company_name
    ON departments(company_id, name)
    WHERE company_id IS NOT NULL AND name IS NOT NULL;

-- =========================
-- 3) users
-- =========================
CREATE TABLE IF NOT EXISTS users (
    user_id         varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    email           varchar(255),
    password_hash   varchar(255),
    full_name       varchar(255),
    phone           varchar(30),
    status          varchar(30) DEFAULT 'ACTIVE',
    last_login_at   timestamptz,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_users_company
    ON users(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_company_email
    ON users(company_id, email)
    WHERE company_id IS NOT NULL AND email IS NOT NULL;

-- =========================
-- 4) roles
-- =========================
CREATE TABLE IF NOT EXISTS roles (
    role_id      varchar(36) PRIMARY KEY,
    company_id   varchar(36),
    code         varchar(50),  -- HR|MANAGER|IT|NEW_EMP|ADMIN...
    name         varchar(255),
    description  varchar(500),
    status       varchar(30) DEFAULT 'ACTIVE',
    created_at   timestamptz DEFAULT now(),
    updated_at   timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_roles_company
    ON roles(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_company_code
    ON roles(company_id, code)
    WHERE company_id IS NOT NULL AND code IS NOT NULL;

-- =========================
-- 5) permissions
-- =========================
CREATE TABLE IF NOT EXISTS permissions (
    permission_id varchar(36) PRIMARY KEY,
    company_id    varchar(36),
    code          varchar(100),
    description   varchar(500),
    status        varchar(30) DEFAULT 'ACTIVE',
    created_at    timestamptz DEFAULT now(),
    updated_at    timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_permissions_company
    ON permissions(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_permissions_company_code
    ON permissions(company_id, code)
    WHERE company_id IS NOT NULL AND code IS NOT NULL;

-- =========================
-- 6) user_roles
-- =========================
CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id varchar(36) PRIMARY KEY,
    company_id   varchar(36),
    user_id      varchar(36),
    role_id      varchar(36),
    created_at   timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_user_roles_company
    ON user_roles(company_id);

CREATE INDEX IF NOT EXISTS ix_user_roles_user
    ON user_roles(company_id, user_id);

CREATE INDEX IF NOT EXISTS ix_user_roles_role
    ON user_roles(company_id, role_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_company_user_role
    ON user_roles(company_id, user_id, role_id)
    WHERE company_id IS NOT NULL AND user_id IS NOT NULL AND role_id IS NOT NULL;

-- =========================
-- 7) role_permissions
-- =========================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_permission_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    role_id            varchar(36),
    permission_id      varchar(36),
    created_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_role_permissions_company
    ON role_permissions(company_id);

CREATE INDEX IF NOT EXISTS ix_role_permissions_role
    ON role_permissions(company_id, role_id);

CREATE INDEX IF NOT EXISTS ix_role_permissions_permission
    ON role_permissions(company_id, permission_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_role_permissions_company_role_permission
    ON role_permissions(company_id, role_id, permission_id)
    WHERE company_id IS NOT NULL AND role_id IS NOT NULL AND permission_id IS NOT NULL;

-- =========================
-- 8) employee_profiles
-- =========================
CREATE TABLE IF NOT EXISTS employee_profiles (
    employee_id       varchar(36) PRIMARY KEY,
    company_id        varchar(36),
    user_id           varchar(36),
    employee_code     varchar(50),
    employee_name     varchar(255),
    employee_email    varchar(255),
    employee_phone    varchar(30),
    job_title         varchar(255),
    manager_user_id   varchar(36),
    department_id     varchar(36),
    start_date        date,
    work_location     varchar(255),
    status            varchar(30) DEFAULT 'ACTIVE',
    created_at        timestamptz DEFAULT now(),
    updated_at        timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_employee_profiles_company
    ON employee_profiles(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_profiles_company_employee_code
    ON employee_profiles(company_id, employee_code)
    WHERE company_id IS NOT NULL AND employee_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_profiles_company_user
    ON employee_profiles(company_id, user_id)
    WHERE company_id IS NOT NULL AND user_id IS NOT NULL;

-- =========================
-- 9) onboarding_templates
-- =========================
CREATE TABLE IF NOT EXISTS onboarding_templates (
                                                    onboarding_template_id varchar(36) PRIMARY KEY,
    company_id             varchar(36),
    name                   varchar(255),
    description            text,
    status                 varchar(30) DEFAULT 'ACTIVE',
    created_by             varchar(36),
    created_at             timestamptz DEFAULT now(),
    updated_at             timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_onboarding_templates_company
    ON onboarding_templates(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_onboarding_templates_company_name
    ON onboarding_templates(company_id, name)
    WHERE company_id IS NOT NULL AND name IS NOT NULL;

-- =========================
-- 10) checklist_templates
-- =========================
CREATE TABLE IF NOT EXISTS checklist_templates (
                                                   checklist_template_id   varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    onboarding_template_id  varchar(36),
    name                    varchar(255),
    stage                   varchar(30), -- PRE|DAY1|POST|D7|D30|D60|CUSTOM
    sort_order              int DEFAULT 0,
    status                  varchar(30) DEFAULT 'ACTIVE',
    created_at              timestamptz DEFAULT now(),
    updated_at              timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_checklist_templates_company
    ON checklist_templates(company_id);

CREATE INDEX IF NOT EXISTS ix_checklist_templates_template
    ON checklist_templates(company_id, onboarding_template_id);

-- =========================
-- 11) task_templates
-- =========================
CREATE TABLE IF NOT EXISTS task_templates (
                                              task_template_id        varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    checklist_template_id   varchar(36),
    title                   varchar(255),
    description             text,
    owner_type              varchar(30) DEFAULT 'DEPARTMENT', -- USER|DEPARTMENT|ROLE
    owner_ref_id            varchar(36),
    due_days_offset         int DEFAULT 0,
    require_ack             boolean DEFAULT false,
    sort_order              int DEFAULT 0,
    status                  varchar(30) DEFAULT 'ACTIVE',
    created_at              timestamptz DEFAULT now(),
    updated_at              timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_task_templates_company
    ON task_templates(company_id);

CREATE INDEX IF NOT EXISTS ix_task_templates_checklist_template
    ON task_templates(company_id, checklist_template_id);

-- =========================
-- 12) onboarding_instances
-- =========================
CREATE TABLE IF NOT EXISTS onboarding_instances (
                                                    onboarding_id           varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    employee_id             varchar(36),
    onboarding_template_id  varchar(36),
    status                  varchar(30) DEFAULT 'DRAFT', -- DRAFT|ACTIVE|DONE|CANCELLED
    start_date              date,
    completed_at            timestamptz,
    created_by              varchar(36),
    created_at              timestamptz DEFAULT now(),
    updated_at              timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_onboarding_instances_company
    ON onboarding_instances(company_id);

CREATE INDEX IF NOT EXISTS ix_onboarding_instances_employee
    ON onboarding_instances(company_id, employee_id);

-- =========================
-- 13) checklist_instances
-- =========================
CREATE TABLE IF NOT EXISTS checklist_instances (
                                                   checklist_id       varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    onboarding_id      varchar(36),
    name               varchar(255),
    stage              varchar(30),
    status             varchar(30) DEFAULT 'NOT_STARTED', -- NOT_STARTED|IN_PROGRESS|DONE
    progress_percent   int DEFAULT 0,
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_checklist_instances_company
    ON checklist_instances(company_id);

CREATE INDEX IF NOT EXISTS ix_checklist_instances_onboarding
    ON checklist_instances(company_id, onboarding_id);

-- =========================
-- 14) task_instances
-- =========================
CREATE TABLE IF NOT EXISTS task_instances (
                                              task_id                 varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    checklist_id            varchar(36),
    task_template_id        varchar(36),
    title                   varchar(255),
    description             text,
    status                  varchar(30) DEFAULT 'TODO', -- TODO|DOING|DONE|BLOCKED|SKIPPED
    due_date                date,
    assigned_user_id        varchar(36),
    assigned_department_id  varchar(36),
    completed_at            timestamptz,
    created_by              varchar(36),
    created_at              timestamptz DEFAULT now(),
    updated_at              timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_task_instances_company
    ON task_instances(company_id);

CREATE INDEX IF NOT EXISTS ix_task_instances_checklist
    ON task_instances(company_id, checklist_id);

CREATE INDEX IF NOT EXISTS ix_task_instances_assigned_user
    ON task_instances(company_id, assigned_user_id);

CREATE INDEX IF NOT EXISTS ix_task_instances_assigned_dept
    ON task_instances(company_id, assigned_department_id);

CREATE INDEX IF NOT EXISTS ix_task_instances_due_date
    ON task_instances(company_id, due_date);

-- =========================
-- 15) task_comments
-- =========================
CREATE TABLE IF NOT EXISTS task_comments (
                                             task_comment_id varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    task_id         varchar(36),
    content         text,
    created_by      varchar(36),
    created_at      timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_task_comments_company
    ON task_comments(company_id);

CREATE INDEX IF NOT EXISTS ix_task_comments_task
    ON task_comments(company_id, task_id);

-- =========================
-- 16) task_activity_logs
-- =========================
CREATE TABLE IF NOT EXISTS task_activity_logs (
                                                  task_activity_log_id  varchar(36) PRIMARY KEY,
    company_id            varchar(36),
    task_id               varchar(36),
    actor_user_id         varchar(36),
    action                varchar(100), -- STATUS_CHANGED, REASSIGNED...
    old_value             text,
    new_value             text,
    created_at            timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_task_activity_logs_company
    ON task_activity_logs(company_id);

CREATE INDEX IF NOT EXISTS ix_task_activity_logs_task
    ON task_activity_logs(company_id, task_id);

-- =========================
-- 17) task_attachments
-- =========================
CREATE TABLE IF NOT EXISTS task_attachments (
                                                task_attachment_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    task_id            varchar(36),
    file_name          varchar(255),
    file_url           text,
    file_type          varchar(100),
    file_size_bytes    bigint,
    uploaded_by        varchar(36),
    uploaded_at        timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_task_attachments_company
    ON task_attachments(company_id);

CREATE INDEX IF NOT EXISTS ix_task_attachments_task
    ON task_attachments(company_id, task_id);

-- =========================
-- 18) onboarding_events
-- =========================
CREATE TABLE IF NOT EXISTS onboarding_events (
                                                 onboarding_event_id varchar(36) PRIMARY KEY,
    company_id          varchar(36),
    onboarding_id       varchar(36),
    actor_user_id       varchar(36),
    event_type          varchar(100), -- STARTED, COMPLETED...
    event_data          jsonb,
    created_at          timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_onboarding_events_company
    ON onboarding_events(company_id);

CREATE INDEX IF NOT EXISTS ix_onboarding_events_onboarding
    ON onboarding_events(company_id, onboarding_id);

-- =========================
-- 19) notifications
-- =========================
CREATE TABLE IF NOT EXISTS notifications (
                                             notification_id  varchar(36) PRIMARY KEY,
    company_id       varchar(36),
    user_id          varchar(36),
    type             varchar(50), -- TASK|ONBOARDING|SURVEY|BILLING...
    title            varchar(255),
    content          text,
    status           varchar(30) DEFAULT 'UNREAD', -- UNREAD|READ
    ref_type         varchar(50),
    ref_id           varchar(36),
    created_at       timestamptz DEFAULT now(),
    read_at          timestamptz
    );

CREATE INDEX IF NOT EXISTS ix_notifications_company
    ON notifications(company_id);

CREATE INDEX IF NOT EXISTS ix_notifications_user
    ON notifications(company_id, user_id);

CREATE INDEX IF NOT EXISTS ix_notifications_status
    ON notifications(company_id, status);

-- =========================
-- 20) email_templates
-- =========================
CREATE TABLE IF NOT EXISTS email_templates (
                                               email_template_id varchar(36) PRIMARY KEY,
    company_id        varchar(36),
    name              varchar(255),
    subject_template  varchar(255),
    body_template     text,
    status            varchar(30) DEFAULT 'ACTIVE',
    created_by        varchar(36),
    created_at        timestamptz DEFAULT now(),
    updated_at        timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_email_templates_company
    ON email_templates(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_email_templates_company_name
    ON email_templates(company_id, name)
    WHERE company_id IS NOT NULL AND name IS NOT NULL;

-- =========================
-- 21) email_campaigns
-- =========================
CREATE TABLE IF NOT EXISTS email_campaigns (
                                               email_campaign_id  varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    email_template_id  varchar(36),
    name               varchar(255),
    trigger_type       varchar(50) DEFAULT 'CUSTOM', -- WELCOME|REMINDER|PAYMENT_FAILED...
    status             varchar(30) DEFAULT 'ACTIVE',
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_email_campaigns_company
    ON email_campaigns(company_id);

-- =========================
-- 22) automation_rules
-- =========================
CREATE TABLE IF NOT EXISTS automation_rules (
                                                automation_rule_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    name               varchar(255),
    rule_type          varchar(50),  -- TASK_DUE_REMINDER|ONBOARDING_START|SURVEY_SCHEDULE...
    email_campaign_id  varchar(36),
    action_type        varchar(50) DEFAULT 'EMAIL', -- EMAIL|NOTIFICATION|BOTH
    condition_json     jsonb,
    schedule_json      jsonb,
    status             varchar(30) DEFAULT 'ACTIVE',
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_automation_rules_company
    ON automation_rules(company_id);

-- =========================
-- 23) email_logs
-- =========================
CREATE TABLE IF NOT EXISTS email_logs (
                                          email_log_id       varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    email_campaign_id  varchar(36),
    onboarding_id      varchar(36),
    user_id            varchar(36),
    to_email           varchar(255),
    subject            varchar(255),
    provider           varchar(50) DEFAULT 'SMTP', -- SMTP|SENDGRID|...
    status             varchar(30) DEFAULT 'QUEUED', -- QUEUED|SENT|FAILED
    error_message      text,
    queued_at          timestamptz DEFAULT now(),
    sent_at            timestamptz
    );

CREATE INDEX IF NOT EXISTS ix_email_logs_company
    ON email_logs(company_id);

CREATE INDEX IF NOT EXISTS ix_email_logs_campaign
    ON email_logs(company_id, email_campaign_id);

CREATE INDEX IF NOT EXISTS ix_email_logs_user
    ON email_logs(company_id, user_id);

-- =========================
-- 24) document_categories
-- =========================
CREATE TABLE IF NOT EXISTS document_categories (
                                                   document_category_id varchar(36) PRIMARY KEY,
    company_id           varchar(36),
    name                 varchar(255),
    description          varchar(500),
    status               varchar(30) DEFAULT 'ACTIVE',
    created_at           timestamptz DEFAULT now(),
    updated_at           timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_document_categories_company
    ON document_categories(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_categories_company_name
    ON document_categories(company_id, name)
    WHERE company_id IS NOT NULL AND name IS NOT NULL;

-- =========================
-- 25) documents
-- =========================
CREATE TABLE IF NOT EXISTS documents (
                                         document_id           varchar(36) PRIMARY KEY,
    company_id            varchar(36),
    document_category_id  varchar(36),
    title                 varchar(255),
    description           text,
    visibility            varchar(30) DEFAULT 'ALL', -- ALL|ROLE|DEPT|CUSTOM
    status                varchar(30) DEFAULT 'ACTIVE',
    created_by            varchar(36),
    created_at            timestamptz DEFAULT now(),
    updated_at            timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_documents_company
    ON documents(company_id);

CREATE INDEX IF NOT EXISTS ix_documents_category
    ON documents(company_id, document_category_id);

-- =========================
-- 26) document_versions
-- =========================
CREATE TABLE IF NOT EXISTS document_versions (
                                                 document_version_id varchar(36) PRIMARY KEY,
    company_id          varchar(36),
    document_id         varchar(36),
    version_no          int,
    file_url            text,
    file_name           varchar(255),
    file_type           varchar(100),
    file_size_bytes     bigint,
    uploaded_by         varchar(36),
    uploaded_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_document_versions_company
    ON document_versions(company_id);

CREATE INDEX IF NOT EXISTS ix_document_versions_document
    ON document_versions(company_id, document_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_versions_document_version
    ON document_versions(company_id, document_id, version_no)
    WHERE company_id IS NOT NULL AND document_id IS NOT NULL AND version_no IS NOT NULL;

-- =========================
-- 27) document_access_rules
-- =========================
CREATE TABLE IF NOT EXISTS document_access_rules (
                                                     document_access_rule_id varchar(36) PRIMARY KEY,
    company_id              varchar(36),
    document_id             varchar(36),
    role_id                 varchar(36),
    department_id           varchar(36),
    status                  varchar(30) DEFAULT 'ACTIVE',
    created_at              timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_document_access_rules_company
    ON document_access_rules(company_id);

CREATE INDEX IF NOT EXISTS ix_document_access_rules_document
    ON document_access_rules(company_id, document_id);

-- =========================
-- 28) document_acknowledgements
-- =========================
CREATE TABLE IF NOT EXISTS document_acknowledgements (
                                                         document_acknowledgement_id varchar(36) PRIMARY KEY,
    company_id                  varchar(36),
    document_id                 varchar(36),
    user_id                     varchar(36),
    onboarding_id               varchar(36),
    status                      varchar(30) DEFAULT 'NOT_READ', -- NOT_READ|READ|ACKED
    read_at                     timestamptz,
    acked_at                    timestamptz,
    created_at                  timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_document_ack_company
    ON document_acknowledgements(company_id);

CREATE INDEX IF NOT EXISTS ix_document_ack_user
    ON document_acknowledgements(company_id, user_id);

-- =========================
-- 29) survey_templates
-- =========================
CREATE TABLE IF NOT EXISTS survey_templates (
                                                survey_template_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    name               varchar(255),
    stage              varchar(30) DEFAULT 'D7', -- D7|D30|D60|CUSTOM
    manager_only       boolean DEFAULT false,
    status             varchar(30) DEFAULT 'ACTIVE',
    created_by         varchar(36),
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_survey_templates_company
    ON survey_templates(company_id);

-- =========================
-- 30) survey_questions
-- =========================
CREATE TABLE IF NOT EXISTS survey_questions (
                                                survey_question_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    survey_template_id varchar(36),
    type               varchar(30), -- RATING|TEXT|CHOICE
    content            text,
    required           boolean DEFAULT false,
    sort_order         int DEFAULT 0,
    options_json       jsonb,
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_survey_questions_company
    ON survey_questions(company_id);

CREATE INDEX IF NOT EXISTS ix_survey_questions_template
    ON survey_questions(company_id, survey_template_id);

-- =========================
-- 31) survey_instances
-- =========================
CREATE TABLE IF NOT EXISTS survey_instances (
                                                survey_instance_id varchar(36) PRIMARY KEY,
    company_id         varchar(36),
    onboarding_id      varchar(36),
    survey_template_id varchar(36),
    scheduled_at       timestamptz,
    sent_at            timestamptz,
    closed_at          timestamptz,
    status             varchar(30) DEFAULT 'SCHEDULED', -- SCHEDULED|SENT|CLOSED
    created_at         timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_survey_instances_company
    ON survey_instances(company_id);

CREATE INDEX IF NOT EXISTS ix_survey_instances_onboarding
    ON survey_instances(company_id, onboarding_id);

-- =========================
-- 32) survey_responses
-- =========================
CREATE TABLE IF NOT EXISTS survey_responses (
                                                survey_response_id  varchar(36) PRIMARY KEY,
    company_id          varchar(36),
    survey_instance_id  varchar(36),
    responder_user_id   varchar(36),
    overall_score       numeric(5,2),
    submitted_at        timestamptz,
    created_at          timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_survey_responses_company
    ON survey_responses(company_id);

CREATE INDEX IF NOT EXISTS ix_survey_responses_instance
    ON survey_responses(company_id, survey_instance_id);

CREATE INDEX IF NOT EXISTS ix_survey_responses_responder
    ON survey_responses(company_id, responder_user_id);

-- =========================
-- 33) survey_answers
-- =========================
CREATE TABLE IF NOT EXISTS survey_answers (
                                              survey_answer_id    varchar(36) PRIMARY KEY,
    company_id          varchar(36),
    survey_response_id  varchar(36),
    survey_question_id  varchar(36),
    value_text          text,
    value_rating        int,
    value_choice        varchar(255),
    created_at          timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_survey_answers_company
    ON survey_answers(company_id);

CREATE INDEX IF NOT EXISTS ix_survey_answers_response
    ON survey_answers(company_id, survey_response_id);

-- =========================
-- 34) kb_articles
-- =========================
CREATE TABLE IF NOT EXISTS kb_articles (
                                           kb_article_id varchar(36) PRIMARY KEY,
    company_id    varchar(36),
    title         varchar(255),
    content       text,
    status        varchar(30) DEFAULT 'DRAFT', -- DRAFT|PUBLISHED
    created_by    varchar(36),
    created_at    timestamptz DEFAULT now(),
    updated_at    timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_kb_articles_company
    ON kb_articles(company_id);

-- =========================
-- 35) tags
-- =========================
CREATE TABLE IF NOT EXISTS tags (
                                    tag_id      varchar(36) PRIMARY KEY,
    company_id  varchar(36),
    name        varchar(100),
    created_at  timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_tags_company
    ON tags(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_company_name
    ON tags(company_id, name)
    WHERE company_id IS NOT NULL AND name IS NOT NULL;

-- =========================
-- 36) kb_article_tags
-- =========================
CREATE TABLE IF NOT EXISTS kb_article_tags (
                                               kb_article_tag_id varchar(36) PRIMARY KEY,
    company_id        varchar(36),
    kb_article_id     varchar(36),
    tag_id            varchar(36),
    created_at        timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_kb_article_tags_company
    ON kb_article_tags(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_kb_article_tags_company_article_tag
    ON kb_article_tags(company_id, kb_article_id, tag_id)
    WHERE company_id IS NOT NULL AND kb_article_id IS NOT NULL AND tag_id IS NOT NULL;

-- =========================
-- 37) chat_sessions
-- =========================
CREATE TABLE IF NOT EXISTS chat_sessions (
                                             chat_session_id varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    user_id         varchar(36),
    channel         varchar(30) DEFAULT 'WEB', -- WEB|MOBILE
    started_at      timestamptz DEFAULT now(),
    ended_at        timestamptz
    );

CREATE INDEX IF NOT EXISTS ix_chat_sessions_company
    ON chat_sessions(company_id);

CREATE INDEX IF NOT EXISTS ix_chat_sessions_user
    ON chat_sessions(company_id, user_id);

-- =========================
-- 38) chat_messages
-- =========================
CREATE TABLE IF NOT EXISTS chat_messages (
                                             chat_message_id varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    chat_session_id varchar(36),
    sender          varchar(10), -- USER|BOT
    content         text,
    kb_article_id   varchar(36),
    created_at      timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_chat_messages_company
    ON chat_messages(company_id);

CREATE INDEX IF NOT EXISTS ix_chat_messages_session
    ON chat_messages(company_id, chat_session_id);

-- =========================
-- 39) plans
-- =========================
CREATE TABLE IF NOT EXISTS plans (
                                     plan_id                  varchar(36) PRIMARY KEY,
    company_id               varchar(36), -- nullable; keep flexible (global vs tenant)
    code                     varchar(50), -- FREE|PRO|BUSINESS|ENTERPRISE
    name                     varchar(255),
    employee_limit_per_month int,
    price_vnd_monthly        int,
    price_vnd_yearly         int,
    status                   varchar(30) DEFAULT 'ACTIVE',
    created_at               timestamptz DEFAULT now(),
    updated_at               timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_plans_company
    ON plans(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plans_company_code
    ON plans(company_id, code)
    WHERE company_id IS NOT NULL AND code IS NOT NULL;

-- =========================
-- 40) plan_features
-- =========================
CREATE TABLE IF NOT EXISTS plan_features (
                                             plan_feature_id varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    plan_id         varchar(36),
    feature_code    varchar(100), -- AI_CHATBOT|API_INTEGRATION|BRANDING|SSO...
    enabled         boolean DEFAULT true,
    created_at      timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_plan_features_company
    ON plan_features(company_id);

CREATE INDEX IF NOT EXISTS ix_plan_features_plan
    ON plan_features(company_id, plan_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plan_features_company_plan_feature
    ON plan_features(company_id, plan_id, feature_code)
    WHERE company_id IS NOT NULL AND plan_id IS NOT NULL AND feature_code IS NOT NULL;

-- =========================
-- 41) subscriptions
-- =========================
CREATE TABLE IF NOT EXISTS subscriptions (
                                             subscription_id        varchar(36) PRIMARY KEY,
    company_id             varchar(36),
    plan_id                varchar(36),
    billing_cycle          varchar(20) DEFAULT 'MONTHLY', -- MONTHLY|YEARLY
    status                 varchar(30) DEFAULT 'ACTIVE', -- ACTIVE|PAST_DUE|CANCELLED|SUSPENDED
    current_period_start   date,
    current_period_end     date,
    auto_renew             boolean DEFAULT true,
    created_at             timestamptz DEFAULT now(),
    updated_at             timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_subscriptions_company
    ON subscriptions(company_id);

CREATE INDEX IF NOT EXISTS ix_subscriptions_plan
    ON subscriptions(company_id, plan_id);

-- =========================
-- 42) usage_monthly
-- =========================
CREATE TABLE IF NOT EXISTS usage_monthly (
                                             usage_monthly_id           varchar(36) PRIMARY KEY,
    company_id                 varchar(36),
    subscription_id            varchar(36),
    month                      varchar(7), -- YYYY-MM
    onboarded_employee_count   int DEFAULT 0,
    updated_at                 timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_usage_monthly_company
    ON usage_monthly(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_monthly_company_sub_month
    ON usage_monthly(company_id, subscription_id, month)
    WHERE company_id IS NOT NULL AND subscription_id IS NOT NULL AND month IS NOT NULL;

-- =========================
-- 43) invoices
-- =========================
CREATE TABLE IF NOT EXISTS invoices (
                                        invoice_id       varchar(36) PRIMARY KEY,
    company_id       varchar(36),
    subscription_id  varchar(36),
    invoice_no       varchar(50),
    amount_total     int,
    currency         varchar(10) DEFAULT 'VND',
    status           varchar(30) DEFAULT 'ISSUED', -- DRAFT|ISSUED|PAID|VOID
    issued_at        timestamptz,
    due_at           timestamptz,
    e_invoice_url    text,
    created_at       timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_invoices_company
    ON invoices(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_company_invoice_no
    ON invoices(company_id, invoice_no)
    WHERE company_id IS NOT NULL AND invoice_no IS NOT NULL;

-- =========================
-- 44) payment_transactions
-- =========================
CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    payment_transaction_id varchar(36) PRIMARY KEY,
    company_id             varchar(36),
    subscription_id        varchar(36),
    invoice_id             varchar(36),
    provider               varchar(30), -- MOMO|ZALOPAY|VNPAY|STRIPE
    provider_txn_id        varchar(100),
    amount                 int,
    status                 varchar(30) DEFAULT 'INIT', -- INIT|SUCCESS|FAILED|REFUNDED
    failure_reason         text,
    created_at             timestamptz DEFAULT now(),
    paid_at                timestamptz
    );

CREATE INDEX IF NOT EXISTS ix_payment_transactions_company
    ON payment_transactions(company_id);

CREATE INDEX IF NOT EXISTS ix_payment_transactions_invoice
    ON payment_transactions(company_id, invoice_id);

CREATE INDEX IF NOT EXISTS ix_payment_transactions_subscription
    ON payment_transactions(company_id, subscription_id);

-- =========================
-- 45) discount_codes
-- =========================
CREATE TABLE IF NOT EXISTS discount_codes (
                                              discount_code_id varchar(36) PRIMARY KEY,
    company_id       varchar(36),
    code             varchar(50),
    description      varchar(500),
    discount_type    varchar(20), -- PERCENT|AMOUNT
    discount_value   numeric(12,2),
    max_redemptions  int,
    redeemed_count   int DEFAULT 0,
    valid_from       timestamptz,
    valid_to         timestamptz,
    status           varchar(30) DEFAULT 'ACTIVE',
    created_at       timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_discount_codes_company
    ON discount_codes(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_discount_codes_company_code
    ON discount_codes(company_id, code)
    WHERE company_id IS NOT NULL AND code IS NOT NULL;

-- =========================
-- 46) subscription_discounts
-- =========================
CREATE TABLE IF NOT EXISTS subscription_discounts (
                                                      subscription_discount_id varchar(36) PRIMARY KEY,
    company_id               varchar(36),
    subscription_id          varchar(36),
    discount_code_id         varchar(36),
    applied_at               timestamptz DEFAULT now(),
    valid_to                 timestamptz,
    status                   varchar(30) DEFAULT 'ACTIVE'
    );

CREATE INDEX IF NOT EXISTS ix_subscription_discounts_company
    ON subscription_discounts(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_discounts_company_sub_discount
    ON subscription_discounts(company_id, subscription_id, discount_code_id)
    WHERE company_id IS NOT NULL AND subscription_id IS NOT NULL AND discount_code_id IS NOT NULL;

-- =========================
-- 47) dunning_cases
-- =========================
CREATE TABLE IF NOT EXISTS dunning_cases (
                                             dunning_case_id varchar(36) PRIMARY KEY,
    company_id      varchar(36),
    subscription_id varchar(36),
    retry_count     int DEFAULT 0,
    next_retry_at   timestamptz,
    status          varchar(30) DEFAULT 'OPEN', -- OPEN|RESOLVED|SUSPENDED
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_dunning_cases_company
    ON dunning_cases(company_id);

CREATE INDEX IF NOT EXISTS ix_dunning_cases_subscription
    ON dunning_cases(company_id, subscription_id);
