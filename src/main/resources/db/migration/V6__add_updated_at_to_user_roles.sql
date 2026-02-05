ALTER TABLE user_roles
    ADD COLUMN updated_at timestamptz DEFAULT now();