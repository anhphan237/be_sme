CREATE TABLE IF NOT EXISTS subscription_change_requests (
    subscription_change_request_id varchar(36) PRIMARY KEY,
    company_id                     varchar(36),
    subscription_id                varchar(36),
    old_plan_id                    varchar(36),
    new_plan_id                    varchar(36),
    billing_cycle                  varchar(20),
    invoice_id                     varchar(36),
    status                         varchar(30) DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT|APPLIED|FAILED|CANCELLED
    requested_by                   varchar(36),
    failure_reason                 text,
    requested_at                   timestamptz DEFAULT now(),
    applied_at                     timestamptz,
    created_at                     timestamptz DEFAULT now(),
    updated_at                     timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_sub_change_req_company
    ON subscription_change_requests(company_id);

CREATE INDEX IF NOT EXISTS ix_sub_change_req_subscription
    ON subscription_change_requests(company_id, subscription_id);

CREATE INDEX IF NOT EXISTS ix_sub_change_req_invoice
    ON subscription_change_requests(company_id, invoice_id);

CREATE INDEX IF NOT EXISTS ix_sub_change_req_status
    ON subscription_change_requests(company_id, status);
