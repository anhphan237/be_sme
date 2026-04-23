-- Phase 5: compare published document versions (JSON metadata for FE)

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604250063000001', NULL, 'com.sme.document.version.compare', 'Compare two document version JSON payloads (metadata)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
