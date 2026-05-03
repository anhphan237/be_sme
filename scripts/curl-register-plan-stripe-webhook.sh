#!/usr/bin/env bash
# Smoke: register company (FREE) → plan list / current subscription / plan details → optional Stripe webhook.
# Requires: curl, jq. Default base URL http://localhost:8080
#
# Usage:
#   export BASE_URL=http://localhost:8080
#   export REGISTER_PLAN=PRO   # optional; default FREE (no upgrade invoice)
#   ./scripts/curl-register-plan-stripe-webhook.sh
#
# Stripe webhook:
# - If app.stripe.webhook-secret is empty, no Stripe-Signature header is required.
# - If webhook secret is set, generate signature (example):
#     PAYLOAD='{"id":"evt_test","type":"payment_intent.succeeded","data":{"object":{"id":"pi_xxx"}}}'
#     TS=$(date +%s)
#     SIG=$(printf '%s' "$TS.$PAYLOAD" | openssl dgst -sha256 -hmac "$STRIPE_WEBHOOK_SECRET" | awk '{print $2}')
#     curl ... -H "Stripe-Signature: t=$TS,v1=$SIG" -d "$PAYLOAD"

set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
SUFFIX="$(date +%s)$RANDOM"
EMAIL="admin-${SUFFIX}@smoke.test"
TAX="SMOKE-TAX-${SUFFIX}"
REGISTER_PLAN="${REGISTER_PLAN:-FREE}"

echo "=== 1) Register company (no Authorization) ==="
REGISTER_JSON=$(curl -sS -X POST "${BASE_URL}/api/v1/gateway" \
  -H "Content-Type: application/json" \
  -d "$(jq -n \
    --arg email "$EMAIL" \
    --arg tax "$TAX" \
    --arg plan "$REGISTER_PLAN" \
    '{
      operationType: "com.sme.company.register",
      requestId: ("req-reg-" + $tax),
      tenantId: null,
      payload: {
        company: {
          name: ("Smoke Co " + $tax),
          taxCode: $tax,
          address: "1 Test St",
          timezone: "Asia/Ho_Chi_Minh"
        },
        admin: {
          username: $email,
          password: "SmokeTest1!",
          fullName: "Smoke Admin",
          phone: "0900000000"
        },
        planCode: $plan,
        billingCycle: "MONTHLY"
      }
    }')")

echo "$REGISTER_JSON" | jq .
TOKEN=$(echo "$REGISTER_JSON" | jq -r '.data.accessToken // empty')
COMPANY_ID=$(echo "$REGISTER_JSON" | jq -r '.data.companyId // empty')
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "Register failed (no accessToken). Fix env/DB and retry." >&2
  exit 1
fi
echo "TOKEN (first 20 chars): ${TOKEN:0:20}..."
echo "COMPANY_ID: $COMPANY_ID"

echo
echo "=== 2) List plans (public — no Authorization) ==="
curl -sS -X POST "${BASE_URL}/api/v1/gateway" \
  -H "Content-Type: application/json" \
  -d '{
    "operationType": "com.sme.billing.plan.list",
    "requestId": "req-plans-1",
    "tenantId": null,
    "payload": {}
  }' | jq .

echo
echo "=== 3) Current subscription (Bearer + tenant from JWT) ==="
curl -sS -X POST "${BASE_URL}/api/v1/gateway" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "operationType": "com.sme.billing.subscription.getCurrent",
    "requestId": "req-sub-current-1",
    "tenantId": null,
    "payload": {}
  }' | jq .

echo
echo "=== 4) Plan details for current subscription (Bearer) ==="
curl -sS -X POST "${BASE_URL}/api/v1/gateway" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "operationType": "com.sme.billing.plan.get",
    "requestId": "req-plan-get-1",
    "tenantId": null,
    "payload": {}
  }' | jq .

echo
echo "=== 5) Stripe webhook (payment_intent.succeeded) ==="
echo "Replace pi_xxx with payment_transactions.provider_txn_id from your DB (after createIntent)."
PI_ID="${STRIPE_TEST_PI_ID:-pi_replace_with_real_intent_id}"
WEBHOOK_BODY=$(jq -n --arg id "$PI_ID" '{
  id: "evt_smoke_test",
  type: "payment_intent.succeeded",
  data: { object: { id: $id } }
}')

if [[ -n "${STRIPE_WEBHOOK_SECRET:-}" ]]; then
  TS=$(date +%s)
  SIG=$(printf '%s' "$TS.$WEBHOOK_BODY" | openssl dgst -sha256 -hmac "$STRIPE_WEBHOOK_SECRET" | awk '{print $2}')
  curl -sS -X POST "${BASE_URL}/api/webhook/stripe" \
    -H "Content-Type: application/json" \
    -H "Stripe-Signature: t=${TS},v1=${SIG}" \
    -d "$WEBHOOK_BODY"
else
  curl -sS -X POST "${BASE_URL}/api/webhook/stripe" \
    -H "Content-Type: application/json" \
    -d "$WEBHOOK_BODY"
fi
echo
echo "(When webhook-secret is unset in app, unsigned body is accepted.)"
