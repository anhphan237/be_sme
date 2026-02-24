-- Seed billing plans (global: company_id = NULL)
-- FREE, PRO, BUSINESS, TEST_10K (Base - 20K VND for real payment testing)
INSERT INTO plans (plan_id, company_id, code, name, employee_limit_per_month, price_vnd_monthly, price_vnd_yearly, status, created_at, updated_at)
VALUES
    (gen_random_uuid()::text, NULL, 'FREE', 'Free', 5, 0, 0, 'ACTIVE', now(), now()),
    (gen_random_uuid()::text, NULL, 'PRO', 'Pro', 50, 500000, 5000000, 'ACTIVE', now(), now()),
    (gen_random_uuid()::text, NULL, 'BUSINESS', 'Business', 200, 1500000, 15000000, 'ACTIVE', now(), now()),
    (gen_random_uuid()::text, NULL, 'TEST_10K', 'Base', 1, 20000, 200000, 'ACTIVE', now(), now())
;
