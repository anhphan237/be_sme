-- Check duplicate email (for register flow)
INSERT INTO permissions (permission_id, company_id, code, description, status)
VALUES ('202602120000000050003', NULL, 'com.sme.identity.auth.checkEmailExists', 'Check email exists for register', 'ACTIVE');
