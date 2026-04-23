-- Phase 4: permissions for access rules, comment lifecycle, list already uses existing tables

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604240062000001', NULL, 'com.sme.document.accessRule.add', 'Add document access rule', 'ACTIVE'),
('202604240062000002', NULL, 'com.sme.document.accessRule.remove', 'Remove document access rule', 'ACTIVE'),
('202604240062000003', NULL, 'com.sme.document.accessRule.list', 'List document access rules', 'ACTIVE'),
('202604240062000004', NULL, 'com.sme.document.comment.delete', 'Soft-delete own document comment', 'ACTIVE'),
('202604240062000005', NULL, 'com.sme.document.comment.update', 'Update own document comment', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
