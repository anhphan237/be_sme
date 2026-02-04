-- =====================================================
-- GLOBAL PERMISSION CATALOG
-- permission_id: STRING time-based (yyyyMMddHHmmssSSS + seq)
-- company_id = NULL => GLOBAL (shared across all tenants)
-- code format: com.sme.<domain>.<resource>.<action>
-- =====================================================

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES

-- =========================
-- ANALYTICS
-- =========================
('202601280300000010001', NULL, 'com.sme.analytics.company.onboarding.read', 'View company onboarding analytics', 'ACTIVE'),
('202601280300000010002', NULL, 'com.sme.analytics.platform.subscription.read', 'View platform subscription metrics', 'ACTIVE'),

-- =========================
-- BILLING / SUBSCRIPTION
-- =========================
('202601280300000020001', NULL, 'com.sme.billing.invoice.generate', 'Generate invoice', 'ACTIVE'),
('202601280300000020002', NULL, 'com.sme.billing.invoice.read', 'View invoice', 'ACTIVE'),
('202601280300000020003', NULL, 'com.sme.billing.invoice.pay', 'Pay invoice', 'ACTIVE'),
('202601280300000020004', NULL, 'com.sme.billing.subscription.create', 'Create subscription', 'ACTIVE'),
('202601280300000020005', NULL, 'com.sme.billing.subscription.update', 'Update subscription', 'ACTIVE'),
('202601280300000020006', NULL, 'com.sme.billing.subscription.read', 'View subscription', 'ACTIVE'),
('202601280300000020007', NULL, 'com.sme.billing.usage.track', 'Track subscription usage', 'ACTIVE'),
('202601280300000020008', NULL, 'com.sme.billing.usage.read', 'View subscription usage', 'ACTIVE'),

-- =========================
-- COMPANY
-- =========================
('202601280300000030001', NULL, 'com.sme.company.register', 'Register company', 'ACTIVE'),
('202601280300000030002', NULL, 'com.sme.company.create', 'Create company', 'ACTIVE'),
('202601280300000030003', NULL, 'com.sme.company.read', 'View company information', 'ACTIVE'),
('202601280300000030004', NULL, 'com.sme.company.update', 'Update company information', 'ACTIVE'),

-- =========================
-- DEPARTMENT
-- =========================
('202601280300000040001', NULL, 'com.sme.company.department.create', 'Create department', 'ACTIVE'),
('202601280300000040002', NULL, 'com.sme.company.department.read', 'View department', 'ACTIVE'),
('202601280300000040003', NULL, 'com.sme.company.department.update', 'Update department', 'ACTIVE'),
('202601280300000040004', NULL, 'com.sme.company.department.delete', 'Delete or disable department', 'ACTIVE'),
('202601280300000040005', NULL, 'com.sme.company.department.assignUser', 'Assign user to department', 'ACTIVE'),

-- =========================
-- AUTH / SESSION
-- =========================
('202601280300000050001', NULL, 'com.sme.identity.auth.login', 'Login', 'ACTIVE'),
('202601280300000050002', NULL, 'com.sme.identity.auth.logout', 'Logout', 'ACTIVE'),

-- =========================
-- USER
-- =========================
('202601280300000050010', NULL, 'com.sme.identity.user.create', 'Create user', 'ACTIVE'),
('202601280300000050011', NULL, 'com.sme.identity.user.read', 'View user detail', 'ACTIVE'),
('202601280300000050012', NULL, 'com.sme.identity.user.list', 'List users', 'ACTIVE'),
('202601280300000050013', NULL, 'com.sme.identity.user.update', 'Update user', 'ACTIVE'),
('202601280300000050014', NULL, 'com.sme.identity.user.disable', 'Disable or enable user', 'ACTIVE'),

-- =========================
-- ROLE / RBAC
-- =========================
('202601280300000060001', NULL, 'com.sme.identity.role.create', 'Create role', 'ACTIVE'),
('202601280300000060002', NULL, 'com.sme.identity.role.read', 'View role', 'ACTIVE'),
('202601280300000060003', NULL, 'com.sme.identity.role.update', 'Update role', 'ACTIVE'),
('202601280300000060004', NULL, 'com.sme.identity.role.delete', 'Delete or disable role', 'ACTIVE'),
('202601280300000060005', NULL, 'com.sme.identity.role.assign', 'Assign role to user', 'ACTIVE'),
('202601280300000060006', NULL, 'com.sme.identity.role.revoke', 'Revoke role from user', 'ACTIVE'),
('202601280300000060007', NULL, 'com.sme.identity.role.grantPermission', 'Grant permission to role', 'ACTIVE'),
('202601280300000060008', NULL, 'com.sme.identity.role.revokePermission', 'Revoke permission from role', 'ACTIVE'),

-- =========================
-- ONBOARDING INSTANCE
-- =========================
('202601280300000070001', NULL, 'com.sme.onboarding.instance.create', 'Create onboarding instance', 'ACTIVE'),
('202601280300000070002', NULL, 'com.sme.onboarding.instance.read', 'View onboarding instance', 'ACTIVE'),
('202601280300000070003', NULL, 'com.sme.onboarding.instance.update', 'Update onboarding instance', 'ACTIVE'),
('202601280300000070004', NULL, 'com.sme.onboarding.instance.activate', 'Activate onboarding instance', 'ACTIVE'),
('202601280300000070005', NULL, 'com.sme.onboarding.instance.cancel', 'Cancel onboarding instance', 'ACTIVE'),

-- =========================
-- ONBOARDING TASK
-- =========================
('202601280300000070010', NULL, 'com.sme.onboarding.task.generate', 'Generate onboarding tasks', 'ACTIVE'),
('202601280300000070011', NULL, 'com.sme.onboarding.task.assign', 'Assign onboarding task', 'ACTIVE'),
('202601280300000070012', NULL, 'com.sme.onboarding.task.read', 'View onboarding task', 'ACTIVE'),
('202601280300000070013', NULL, 'com.sme.onboarding.task.updateStatus', 'Update onboarding task status', 'ACTIVE'),

-- =========================
-- ONBOARDING TEMPLATE
-- =========================
('202601280300000070020', NULL, 'com.sme.onboarding.template.create', 'Create onboarding template', 'ACTIVE'),
('202601280300000070021', NULL, 'com.sme.onboarding.template.read', 'View onboarding template', 'ACTIVE'),
('202601280300000070022', NULL, 'com.sme.onboarding.template.update', 'Update onboarding template', 'ACTIVE'),
('202601280300000070023', NULL, 'com.sme.onboarding.template.delete', 'Delete onboarding template', 'ACTIVE'),

-- =========================
-- SURVEY
-- =========================
('202601280300000080001', NULL, 'com.sme.survey.instance.schedule', 'Schedule survey instance', 'ACTIVE'),
('202601280300000080002', NULL, 'com.sme.survey.instance.read', 'View survey instance', 'ACTIVE'),
('202601280300000080003', NULL, 'com.sme.survey.response.submit', 'Submit survey response', 'ACTIVE'),
('202601280300000080004', NULL, 'com.sme.survey.template.create', 'Create survey template', 'ACTIVE'),
('202601280300000080005', NULL, 'com.sme.survey.template.read', 'View survey template', 'ACTIVE');

-- Note: conflict handling intentionally omitted if this is the only seed source.
-- Add ON CONFLICT (company_id, code) DO NOTHING if re-runnable is required.
