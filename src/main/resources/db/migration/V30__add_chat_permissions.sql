-- Chat: session create, list, message list
INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202603221200000001', NULL, 'com.sme.chat.session.create', 'Create chat session (new conversation)', 'ACTIVE'),
('202603221200000002', NULL, 'com.sme.chat.session.list', 'List chat sessions for user', 'ACTIVE'),
('202603221200000003', NULL, 'com.sme.chat.message.list', 'List messages in chat session', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
