-- Email template for HR-created user invite (company_id NULL = system-wide)
INSERT INTO email_templates (email_template_id, company_id, name, subject_template, body_template, status, created_at, updated_at) VALUES
('etpl_invite_004', NULL, 'USER_INVITE', 'Welcome to {{companyName}} - Your account has been created', 'Hi {{employeeName}},

Your account has been created at {{companyName}}.

Login credentials:
- Email: {{email}}
- Password: {{password}}

Please log in and change your password after first login.

Best regards',
'ACTIVE', now(), now())
ON CONFLICT (email_template_id) DO NOTHING;
