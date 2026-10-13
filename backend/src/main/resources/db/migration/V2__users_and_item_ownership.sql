CREATE TABLE app_user (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(24) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_user_role (role)
);

ALTER TABLE ewaste_item ADD COLUMN owner_email VARCHAR(254) NULL;
CREATE INDEX idx_item_owner_created ON ewaste_item (owner_email, created_at);
