-- Automation email send permission (Đợt 5 - test send)
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202602061600000050001', NULL, 'com.sme.automation.email.send', 'Send email by template (e.g. test welcome)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
