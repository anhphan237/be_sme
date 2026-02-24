-- Payment providers table: stores connected payment gateway info per company
CREATE TABLE IF NOT EXISTS payment_providers (
    payment_provider_id VARCHAR(64) PRIMARY KEY,
    company_id          VARCHAR(64) NOT NULL,
    provider_name       VARCHAR(50) NOT NULL,  -- e.g. Stripe, MoMo, ZaloPay, VNPay
    status              VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED', -- CONNECTED / DISCONNECTED
    account_id          VARCHAR(128),          -- external account id (e.g. Stripe acct_xxx)
    last_sync_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_providers_company ON payment_providers(company_id);

-- Add currency column to payment_transactions if missing
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS currency VARCHAR(10);

-- Seed default permission rows for new payment operations
INSERT INTO permissions (permission_id, code, description, created_at)
VALUES
    (gen_random_uuid()::text, 'com.sme.billing.payment.connect', 'Connect payment provider', now()),
    (gen_random_uuid()::text, 'com.sme.billing.payment.providers', 'List payment providers', now()),
    (gen_random_uuid()::text, 'com.sme.billing.payment.status', 'Get payment status', now()),
    (gen_random_uuid()::text, 'com.sme.billing.payment.transactions', 'List payment transactions', now())
ON CONFLICT DO NOTHING;
