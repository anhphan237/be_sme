alter table event_instances
    add column if not exists event_end_at timestamptz;

update event_instances
set event_end_at = event_at
where event_end_at is null;
