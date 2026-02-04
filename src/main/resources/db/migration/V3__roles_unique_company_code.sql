-- Make roles(company_id, code) unique so ON CONFLICT (company_id, code) works

-- 0) (optional) Drop old partial index to avoid confusion
DROP INDEX IF EXISTS uq_roles_company_code;

-- 1) Deduplicate existing roles by (company_id, code)
-- Keep the newest by created_at (fallback role_id)
WITH ranked AS (
    SELECT
        role_id,
        ROW_NUMBER() OVER (
        PARTITION BY company_id, code
        ORDER BY created_at DESC NULLS LAST, role_id DESC
      ) AS rn
    FROM roles
    WHERE company_id IS NOT NULL
      AND code IS NOT NULL
)
DELETE FROM roles
WHERE role_id IN (SELECT role_id FROM ranked WHERE rn > 1);

-- 2) Create a UNIQUE index (no WHERE) => can be used by ON CONFLICT target
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_company_code_uk
    ON roles(company_id, code);

-- 3) Add UNIQUE CONSTRAINT using the index (so Postgres recognizes it as a constraint)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uq_roles_company_code'
  ) THEN
ALTER TABLE roles
    ADD CONSTRAINT uq_roles_company_code
        UNIQUE USING INDEX uq_roles_company_code_uk;
END IF;
END $$;
