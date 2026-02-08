INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202601280300000070006', NULL, 'com.sme.onboarding.instance.complete', 'Complete onboarding instance', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
