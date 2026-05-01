alter table survey_templates
    add column if not exists purpose varchar(50);

update survey_templates
set purpose = 'ONBOARDING_FEEDBACK'
where purpose is null;

alter table survey_templates
    alter column purpose set default 'ONBOARDING_FEEDBACK';

alter table survey_instances
    add column if not exists purpose varchar(50);

update survey_instances
set purpose = 'ONBOARDING_FEEDBACK'
where purpose is null;

alter table survey_instances
    alter column purpose set default 'ONBOARDING_FEEDBACK';

alter table survey_instances
    add column if not exists subject_user_id varchar(36);

drop index if exists ux_survey_template_default_per_company_stage_role;

create unique index if not exists ux_survey_template_default_per_company_purpose_stage_role
    on survey_templates (company_id, purpose, stage, target_role)
    where is_default = true;

create index if not exists ix_survey_templates_company_purpose
    on survey_templates (company_id, purpose);

create index if not exists ix_survey_instances_company_purpose
    on survey_instances (company_id, purpose);

create index if not exists ix_survey_instances_subject_user
    on survey_instances (company_id, subject_user_id);