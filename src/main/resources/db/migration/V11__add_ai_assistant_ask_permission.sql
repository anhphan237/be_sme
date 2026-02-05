INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202601280300000040001', NULL, 'com.sme.ai.assistant.ask', 'Ask AI assistant (new employee Q&A using Document Library)', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
