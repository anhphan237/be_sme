INSERT INTO permissions (permission_id, company_id, code, description, status)
VALUES

-- =========================
-- AUTH
-- =========================
('202601280300000010001', NULL, 'AUTH.LOGIN', 'Login to system', 'ACTIVE'),
('202601280300000010002', NULL, 'AUTH.LOGOUT', 'Logout from system', 'ACTIVE'),
('202601280300000010003', NULL, 'AUTH.REFRESH_TOKEN', 'Refresh access token', 'ACTIVE'),

-- =========================
-- COMPANY
-- =========================
('202601280300000020001', NULL, 'COMPANY.REGISTER', 'Register company and first admin', 'ACTIVE'),
('202601280300000020002', NULL, 'COMPANY.READ', 'View company information', 'ACTIVE'),
('202601280300000020003', NULL, 'COMPANY.UPDATE', 'Update company information', 'ACTIVE'),

-- =========================
-- DEPARTMENT
-- =========================
('202601280300000030001', NULL, 'DEPARTMENT.CREATE', 'Create department', 'ACTIVE'),
('202601280300000030002', NULL, 'DEPARTMENT.READ', 'View departments', 'ACTIVE'),
('202601280300000030003', NULL, 'DEPARTMENT.UPDATE', 'Update department', 'ACTIVE'),
('202601280300000030004', NULL, 'DEPARTMENT.DELETE', 'Delete or disable department', 'ACTIVE'),
('202601280300000030005', NULL, 'DEPARTMENT.ASSIGN_USER', 'Assign user to department', 'ACTIVE'),

-- =========================
-- USER
-- =========================
('202601280300000040001', NULL, 'USER.CREATE', 'Create user', 'ACTIVE'),
('202601280300000040002', NULL, 'USER.LIST', 'List users', 'ACTIVE'),
('202601280300000040003', NULL, 'USER.READ', 'View user detail', 'ACTIVE'),
('202601280300000040004', NULL, 'USER.UPDATE', 'Update user', 'ACTIVE'),
('202601280300000040005', NULL, 'USER.DISABLE', 'Disable or enable user', 'ACTIVE'),
('202601280300000040006', NULL, 'USER.ASSIGN_ROLE', 'Assign role to user', 'ACTIVE'),

-- =========================
-- ROLE / PERMISSION ADMIN
-- =========================
('202601280300000050001', NULL, 'ROLE.CREATE', 'Create role', 'ACTIVE'),
('202601280300000050002', NULL, 'ROLE.READ', 'View roles', 'ACTIVE'),
('202601280300000050003', NULL, 'ROLE.UPDATE', 'Update role', 'ACTIVE'),
('202601280300000050004', NULL, 'ROLE.DELETE', 'Delete or disable role', 'ACTIVE'),
('202601280300000050005', NULL, 'ROLE.GRANT_PERMISSION', 'Grant permission to role', 'ACTIVE'),
('202601280300000050006', NULL, 'ROLE.REVOKE_PERMISSION', 'Revoke permission from role', 'ACTIVE'),

-- =========================
-- EMPLOYEE PROFILE
-- =========================
('202601280300000060001', NULL, 'EMPLOYEE_PROFILE.CREATE', 'Create employee profile', 'ACTIVE'),
('202601280300000060002', NULL, 'EMPLOYEE_PROFILE.READ', 'View employee profile', 'ACTIVE'),
('202601280300000060003', NULL, 'EMPLOYEE_PROFILE.UPDATE', 'Update employee profile', 'ACTIVE'),

-- =========================
-- ONBOARDING TEMPLATE
-- =========================
('202601280300000070001', NULL, 'ONBOARDING_TEMPLATE.CREATE', 'Create onboarding template', 'ACTIVE'),
('202601280300000070002', NULL, 'ONBOARDING_TEMPLATE.READ', 'View onboarding templates', 'ACTIVE'),
('202601280300000070003', NULL, 'ONBOARDING_TEMPLATE.UPDATE', 'Update onboarding template', 'ACTIVE'),
('202601280300000070004', NULL, 'ONBOARDING_TEMPLATE.DELETE', 'Delete onboarding template', 'ACTIVE'),

-- =========================
-- ONBOARDING INSTANCE
-- =========================
('202601280300000080001', NULL, 'ONBOARDING_INSTANCE.CREATE', 'Initialize onboarding instance', 'ACTIVE'),
('202601280300000080002', NULL, 'ONBOARDING_INSTANCE.READ', 'View onboarding instance', 'ACTIVE'),
('202601280300000080003', NULL, 'ONBOARDING_INSTANCE.UPDATE', 'Update onboarding instance', 'ACTIVE'),
('202601280300000080004', NULL, 'ONBOARDING_INSTANCE.ACTIVATE', 'Activate onboarding instance', 'ACTIVE'),
('202601280300000080005', NULL, 'ONBOARDING_INSTANCE.CANCEL', 'Cancel onboarding instance', 'ACTIVE'),

-- =========================
-- TASK
-- =========================
('202601280300000090001', NULL, 'TASK.READ', 'View onboarding tasks', 'ACTIVE'),
('202601280300000090002', NULL, 'TASK.UPDATE_STATUS', 'Update task status', 'ACTIVE'),
('202601280300000090003', NULL, 'TASK.ASSIGN_OWNER', 'Assign task owner', 'ACTIVE'),
('202601280300000090004', NULL, 'TASK.SEND_REMINDER', 'Send task reminder', 'ACTIVE'),

-- =========================
-- SURVEY
-- =========================
('202601280300000100001', NULL, 'SURVEY.INSTANCE.CREATE', 'Create survey instance', 'ACTIVE'),
('202601280300000100002', NULL, 'SURVEY.READ', 'Read survey', 'ACTIVE'),
('202601280300000100003', NULL, 'SURVEY.SUBMIT', 'Submit survey', 'ACTIVE'),
('202601280300000100004', NULL, 'SURVEY.REPORT.READ', 'View survey reports', 'ACTIVE'),

-- =========================
-- BILLING
-- =========================
('202601280300000110001', NULL, 'BILLING.SUBSCRIPTION.MANAGE', 'Manage subscription', 'ACTIVE'),
('202601280300000110002', NULL, 'BILLING.INVOICE.READ', 'View invoice', 'ACTIVE'),
('202601280300000110003', NULL, 'BILLING.INVOICE.PAY', 'Pay invoice', 'ACTIVE'),
('202601280300000110004', NULL, 'BILLING.PAYMENT.RETRY', 'Retry payment', 'ACTIVE'),
('202601280300000110005', NULL, 'BILLING.USAGE.READ', 'View usage', 'ACTIVE'),

-- =========================
-- REPORT
-- =========================
('202601280300000120001', NULL, 'REPORT.COMPANY.READ', 'View company reports', 'ACTIVE'),
('202601280300000120002', NULL, 'REPORT.PLATFORM.READ', 'View platform analytics', 'ACTIVE')

    ON CONFLICT (company_id, code) DO NOTHING;
