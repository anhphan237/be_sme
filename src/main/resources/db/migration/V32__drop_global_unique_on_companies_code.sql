-- Prefix company.code is only for display in employee codes [ABC]000001; uniqueness is already
-- enforced per company on employee_profiles (company_id, employee_code). Global uniqueness on
-- code caused many false DUPLICATED errors when names share the same first letters.
DROP INDEX IF EXISTS uq_companies_code;
