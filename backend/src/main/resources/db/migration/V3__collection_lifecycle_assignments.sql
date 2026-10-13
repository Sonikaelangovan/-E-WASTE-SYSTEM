ALTER TABLE ewaste_item
 ADD COLUMN assigned_collector_email VARCHAR(254) NULL,
 ADD COLUMN assigned_recycler_email VARCHAR(254) NULL,
 ADD COLUMN pickup_scheduled_at TIMESTAMP(6) NULL;

ALTER TABLE trace_event MODIFY COLUMN actor VARCHAR(254) NOT NULL;

CREATE INDEX idx_item_assigned_collector ON ewaste_item (assigned_collector_email, status);
CREATE INDEX idx_item_assigned_recycler ON ewaste_item (assigned_recycler_email, status);
