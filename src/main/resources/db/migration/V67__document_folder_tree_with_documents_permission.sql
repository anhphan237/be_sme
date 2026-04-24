-- Permission for folder tree with document nodes

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604251067000001', NULL, 'com.sme.document.folder.treeWithDocuments', 'List folder tree with documents', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
