ALTER TABLE event_instances
    ADD COLUMN IF NOT EXISTS cover_image_url varchar(1000);
