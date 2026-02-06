-- Survey instance list and satisfaction report permissions (Đợt 2)
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202602061300000020001', NULL, 'com.sme.survey.instance.list', 'List survey instances with filters and pagination', 'ACTIVE'),
('202602061300000020002', NULL, 'com.sme.survey.report.satisfaction', 'View satisfaction report aggregated by question', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
