-- Invite tokens for secure set-password flow (password not sent via email)
CREATE TABLE IF NOT EXISTS user_invite_tokens (
    invite_token_id  varchar(36) PRIMARY KEY,
    user_id          varchar(36) NOT NULL,
    token_hash       varchar(64) NOT NULL,
    expires_at       timestamptz NOT NULL,
    created_at       timestamptz DEFAULT now(),
    used_at          timestamptz,
    CONSTRAINT fk_invite_token_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS ix_user_invite_tokens_user ON user_invite_tokens(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_invite_tokens_hash ON user_invite_tokens(token_hash);
