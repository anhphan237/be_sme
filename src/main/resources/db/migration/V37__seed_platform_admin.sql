-- =====================================================
-- PLATFORM ADMIN: company, role, user, user_role
-- Fixed UUIDs for predictability and idempotency.
-- Password is set at application startup via PlatformAdminSeeder.
-- =====================================================

INSERT INTO companies (company_id, name, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'PLATFORM', 'PLATFORM', now(), now())
ON CONFLICT (company_id) DO NOTHING;

INSERT INTO roles (role_id, company_id, code, name, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'ADMIN', 'Platform Admin', 'ACTIVE', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO users (user_id, company_id, email, full_name, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000001',
        'admin@platform.sme', 'Platform Admin', 'ACTIVE', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_role_id, company_id, user_id, role_id, created_at)
VALUES ('00000000-0000-0000-0000-000000000004',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000002',
        now())
ON CONFLICT DO NOTHING;
