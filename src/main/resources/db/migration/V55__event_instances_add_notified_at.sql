-- Legacy DBs may have event_instances without notified_at (V51 uses IF NOT EXISTS on CREATE TABLE).
-- Align with EventInstanceMapper / application expectations.

ALTER TABLE event_instances
    ADD COLUMN IF NOT EXISTS notified_at timestamptz;
