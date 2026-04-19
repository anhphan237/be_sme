-- Email template for HR notification when Stripe payment succeeds (webhook)
INSERT INTO email_templates (email_template_id, company_id, name, subject_template, body_template, status, created_at, updated_at) VALUES
('etpl_pay_ok_047', NULL, 'PAYMENT_SUCCEEDED', 'Payment received: {{invoiceNo}}', 'Hi,\n\nA payment of {{amountTotal}} {{currency}} has been completed successfully for invoice {{invoiceNo}} (company: {{companyName}}).\n\nBest regards', 'ACTIVE', now(), now())
ON CONFLICT (email_template_id) DO NOTHING;
