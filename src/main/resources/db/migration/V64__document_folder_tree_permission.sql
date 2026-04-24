-- Tree view for document folders (same access intent as folder.list)

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604251064000001', NULL, 'com.sme.document.folder.tree', 'List document folders as a tree', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
