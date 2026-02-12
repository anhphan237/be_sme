-- Assign manager (head) to department
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS manager_user_id varchar(36);

CREATE INDEX IF NOT EXISTS ix_departments_manager_user
    ON departments(company_id, manager_user_id)
    WHERE manager_user_id IS NOT NULL;
