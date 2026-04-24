INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202606201200000070036', NULL, 'com.sme.onboarding.task.comment.tree', 'Query comments tree on an onboarding task', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
