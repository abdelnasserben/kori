CREATE INDEX IF NOT EXISTS idx_audit_events_meta_resource_type
    ON audit_events (((metadata_json::jsonb ->> 'resourceType')));

CREATE INDEX IF NOT EXISTS idx_audit_events_meta_resource_ref
    ON audit_events (((metadata_json::jsonb ->> 'resourceRef')));
