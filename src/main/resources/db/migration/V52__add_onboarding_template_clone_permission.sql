INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604210300000070036', NULL, 'com.sme.onboarding.template.clone', 'Clone onboarding template from platform to tenant', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
