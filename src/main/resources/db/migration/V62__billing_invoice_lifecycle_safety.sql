ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS expired_at timestamptz;

UPDATE invoices
SET expired_at = due_at
WHERE expired_at IS NULL
  AND due_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_invoices_status_expired_at
    ON invoices(status, expired_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_transactions_provider_provider_txn
    ON payment_transactions(provider, provider_txn_id)
    WHERE provider IS NOT NULL
      AND provider_txn_id IS NOT NULL
      AND trim(provider_txn_id) <> '';
