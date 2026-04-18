ALTER TABLE onboarding_templates
    ADD COLUMN IF NOT EXISTS template_kind varchar(30) DEFAULT 'ONBOARDING';

ALTER TABLE onboarding_templates
    ADD COLUMN IF NOT EXISTS department_type_code varchar(100);

UPDATE onboarding_templates
SET template_kind = 'ONBOARDING'
WHERE template_kind IS NULL;

CREATE INDEX IF NOT EXISTS ix_onboarding_templates_kind
    ON onboarding_templates(company_id, template_kind);

CREATE INDEX IF NOT EXISTS ix_onboarding_templates_department_type_code
    ON onboarding_templates(company_id, department_type_code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_library_department_type
    ON onboarding_templates(company_id, template_kind, department_type_code)
    WHERE template_kind = 'TASK_LIBRARY'
      AND company_id IS NOT NULL
      AND department_type_code IS NOT NULL;

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000045001', NULL, 'com.sme.onboarding.taskLibrary.list', 'List task libraries', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.onboarding.taskLibrary.list'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000045002', NULL, 'com.sme.onboarding.taskLibrary.get', 'Get task library details', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.onboarding.taskLibrary.get'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000045003', NULL, 'com.sme.onboarding.taskLibrary.downloadExcelTemplate', 'Download task library excel template', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.onboarding.taskLibrary.downloadExcelTemplate'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000045004', NULL, 'com.sme.onboarding.taskLibrary.importExcel', 'Import task library from excel', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.onboarding.taskLibrary.importExcel'
);
