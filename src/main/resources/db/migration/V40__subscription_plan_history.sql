CREATE TABLE IF NOT EXISTS subscription_plan_history (
    subscription_plan_history_id varchar(36) PRIMARY KEY,
    company_id                  varchar(36),
    subscription_id             varchar(36),
    old_plan_id                 varchar(36),
    new_plan_id                 varchar(36),
    billing_cycle               varchar(20),
    changed_by                  varchar(36),
    changed_at                  timestamptz DEFAULT now(),
    effective_from              timestamptz,
    effective_to                timestamptz,
    created_at                  timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_sub_plan_history_company
    ON subscription_plan_history(company_id);

CREATE INDEX IF NOT EXISTS ix_sub_plan_history_subscription
    ON subscription_plan_history(company_id, subscription_id);

CREATE INDEX IF NOT EXISTS ix_sub_plan_history_changed_at
    ON subscription_plan_history(company_id, changed_at DESC);
