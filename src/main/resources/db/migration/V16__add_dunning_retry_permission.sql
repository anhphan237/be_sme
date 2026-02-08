-- Dunning retry permission (Đợt 4)
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202602061500000040001', NULL, 'com.sme.billing.dunning.retry', 'Retry payment for a dunning case', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
