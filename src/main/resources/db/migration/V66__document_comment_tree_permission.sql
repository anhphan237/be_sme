-- Permission for listing document comments in nested tree format

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604251066000001', NULL, 'com.sme.document.comment.tree', 'List document comments as tree', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
