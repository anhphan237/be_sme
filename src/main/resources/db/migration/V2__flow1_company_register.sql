-- Flow 1 - Company Registration
-- Ensure uniqueness for company tax code and case-insensitive name

CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_tax_code
    ON companies(tax_code)
    WHERE tax_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_lower_name
    ON companies(lower(name))
    WHERE name IS NOT NULL;

DROP INDEX IF EXISTS uq_users_company_lower_email;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_lower_email
    ON users(lower(email))
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_company_code
    ON roles(company_id, code)
    WHERE company_id IS NOT NULL AND code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_company_user_role
    ON user_roles(company_id, user_id, role_id)
    WHERE company_id IS NOT NULL AND user_id IS NOT NULL AND role_id IS NOT NULL;
