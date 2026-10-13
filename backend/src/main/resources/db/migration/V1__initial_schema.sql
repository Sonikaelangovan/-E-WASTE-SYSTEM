CREATE TABLE ewaste_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 trace_id VARCHAR(24) NOT NULL UNIQUE,
 name VARCHAR(120) NOT NULL,
 category VARCHAR(80) NOT NULL,
 weight_kg DECIMAL(12,2) NOT NULL,
 address VARCHAR(300) NOT NULL,
 description VARCHAR(1000),
 status VARCHAR(32) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_item_status (status),
 INDEX idx_item_created (created_at)
);
CREATE TABLE trace_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 item_id BIGINT NOT NULL,
 previous_status VARCHAR(32),
 to_status VARCHAR(32) NOT NULL,
 actor VARCHAR(120) NOT NULL,
 notes VARCHAR(1000),
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_trace_item FOREIGN KEY (item_id) REFERENCES ewaste_item(id),
 INDEX idx_trace_item_created (item_id, created_at)
);
