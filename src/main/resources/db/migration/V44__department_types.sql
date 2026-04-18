CREATE TABLE IF NOT EXISTS department_types (
    department_type_id varchar(36) PRIMARY KEY,
    company_id         varchar(36) NOT NULL,
    code               varchar(100) NOT NULL,
    name               varchar(255) NOT NULL,
    status             varchar(30) DEFAULT 'ACTIVE',
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_department_types_company
    ON department_types(company_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_department_types_company_code
    ON department_types(company_id, code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_department_types_company_name
    ON department_types(company_id, name);

-- Backfill existing department.type values so legacy data keeps working.
INSERT INTO department_types (
    department_type_id, company_id, code, name, status, created_at, updated_at
)
SELECT DISTINCT
    ('DPTT-' || substr(md5(d.company_id || ':' || upper(trim(d.type))), 1, 31)) AS department_type_id,
    d.company_id,
    upper(trim(d.type)) AS code,
    upper(trim(d.type)) AS name,
    'ACTIVE',
    now(),
    now()
FROM departments d
WHERE d.company_id IS NOT NULL
  AND d.type IS NOT NULL
  AND trim(d.type) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM department_types dt
    WHERE dt.company_id = d.company_id
      AND dt.code = upper(trim(d.type))
);

-- Ensure each tenant has at least OTHER as a baseline type.
INSERT INTO department_types (
    department_type_id, company_id, code, name, status, created_at, updated_at
)
SELECT
    ('DPTT-' || substr(md5(c.company_id || ':OTHER'), 1, 31)) AS department_type_id,
    c.company_id,
    'OTHER',
    'OTHER',
    'ACTIVE',
    now(),
    now()
FROM companies c
WHERE c.company_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM department_types dt
    WHERE dt.company_id = c.company_id
      AND dt.code = 'OTHER'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000044001', NULL, 'com.sme.company.departmentType.create', 'Create department type', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.company.departmentType.create'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000044002', NULL, 'com.sme.company.departmentType.list', 'List department types', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.company.departmentType.list'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000044003', NULL, 'com.sme.company.departmentType.update', 'Update department type', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.company.departmentType.update'
);

INSERT INTO permissions (permission_id, company_id, code, description, status)
SELECT '20260418000000044004', NULL, 'com.sme.company.departmentType.delete', 'Delete/disable department type', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE company_id IS NULL AND code = 'com.sme.company.departmentType.delete'
);
