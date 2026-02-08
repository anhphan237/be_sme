-- Notification list and mark-read permissions (Đợt 1)
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202602061200000010001', NULL, 'com.sme.notification.list', 'List notifications for current user', 'ACTIVE'),
('202602061200000010002', NULL, 'com.sme.notification.markRead', 'Mark notifications as read', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
