CREATE TABLE app_notification (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 recipient_email VARCHAR(254) NOT NULL,
 item_id BIGINT NULL,
 notification_type VARCHAR(32) NOT NULL,
 title VARCHAR(160) NOT NULL,
 message VARCHAR(500) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 read_at TIMESTAMP(6) NULL,
 CONSTRAINT fk_notification_item FOREIGN KEY (item_id) REFERENCES ewaste_item(id),
 INDEX idx_notification_recipient_created (recipient_email, created_at),
 INDEX idx_notification_recipient_unread (recipient_email, read_at)
);
