INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202601280300000030001', NULL, 'com.sme.content.document.upload', 'Upload document metadata (Employee Handbook etc.)', 'ACTIVE'),
('202601280300000030002', NULL, 'com.sme.content.document.list', 'List documents for tenant', 'ACTIVE'),
('202601280300000030003', NULL, 'com.sme.content.document.acknowledge', 'Acknowledge document read (I have read this)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
