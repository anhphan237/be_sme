alter table plans
    add column if not exists onboarding_template_limit integer;

alter table plans
    add column if not exists event_template_limit integer;

alter table plans
    add column if not exists document_limit integer;

alter table plans
    add column if not exists storage_limit_bytes bigint;
