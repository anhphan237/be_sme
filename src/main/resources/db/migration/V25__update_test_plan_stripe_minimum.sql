-- Stripe minimum for VND ~50 cents USD; 10,000 VND too small.
-- Update TEST_10K to 20,000 VND (meets Stripe minimum).
UPDATE plans
SET price_vnd_monthly = 20000,
    price_vnd_yearly = 200000,
    name = 'Base',
    updated_at = now()
WHERE code = 'TEST_10K' AND company_id IS NULL;
