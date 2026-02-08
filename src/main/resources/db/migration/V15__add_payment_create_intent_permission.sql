-- Payment create intent permission (Đợt 3 - Payment gateway)
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202602061400000030001', NULL, 'com.sme.billing.payment.createIntent', 'Create payment intent for invoice (gateway)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
