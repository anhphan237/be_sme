INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202601280300000020009', NULL, 'com.sme.billing.plan.get', 'Get current plan details', 'ACTIVE'),
('202601280300000020010', NULL, 'com.sme.billing.usage.check', 'Check subscription usage (e.g. onboarding instances this month)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
