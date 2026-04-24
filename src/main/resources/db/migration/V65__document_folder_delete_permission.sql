-- Permission for deleting document folders (soft delete)

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604251065000001', NULL, 'com.sme.document.folder.delete', 'Delete document folder (soft delete)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
