-- Template for invite flow with set-password link (no password in email)
INSERT INTO email_templates (email_template_id, company_id, name, subject_template, body_template, status, created_at, updated_at) VALUES
('etpl_invite_link_005', NULL, 'USER_INVITE_LINK', 'Welcome to {{companyName}} - Set your password', 'Hi {{employeeName}},

Your account has been created at {{companyName}}.

Click the link below to set your password and activate your account:
{{setPasswordLink}}

This link expires in 72 hours. If you did not expect this email, please ignore it.

Best regards',
'ACTIVE', now(), now())
ON CONFLICT (email_template_id) DO NOTHING;
