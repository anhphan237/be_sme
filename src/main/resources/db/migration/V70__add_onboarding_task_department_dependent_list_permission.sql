INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604270900000070038', NULL, 'com.sme.onboarding.task.department.dependent.list', 'List onboarding tasks dependent on department checkpoints', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
