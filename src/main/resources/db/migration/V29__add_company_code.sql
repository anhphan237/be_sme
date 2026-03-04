-- Company code (3 chars) for employee code format [ABC]000001
ALTER TABLE companies ADD COLUMN IF NOT EXISTS code varchar(10);

CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_code
    ON companies(code)
    WHERE code IS NOT NULL;
