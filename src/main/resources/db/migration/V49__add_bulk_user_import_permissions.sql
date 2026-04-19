INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604191600000050015', NULL, 'com.sme.identity.user.bulkImport.downloadTemplate', 'Download bulk user import template', 'ACTIVE'),
('202604191600000050016', NULL, 'com.sme.identity.user.bulkImport.validate', 'Validate bulk user import file', 'ACTIVE'),
('202604191600000050017', NULL, 'com.sme.identity.user.bulkImport.commit', 'Commit bulk user import', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
