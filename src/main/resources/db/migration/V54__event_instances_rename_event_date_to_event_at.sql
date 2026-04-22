-- Align legacy column name with application (EventInstanceMapper / V51 use event_at).
-- Some databases had event_instances created with event_date before V51; CREATE TABLE IF NOT EXISTS
-- would not alter the column. Rename when only event_date exists.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'event_instances'
          AND column_name = 'event_date'
    )
       AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'event_instances'
          AND column_name = 'event_at'
    ) THEN
        ALTER TABLE event_instances RENAME COLUMN event_date TO event_at;
    END IF;
END $$;
