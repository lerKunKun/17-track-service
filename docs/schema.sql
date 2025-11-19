-- 17-track microservice database bootstrap
-- 使用前请根据实际需求调整数据库名、字符集以及账号权限

CREATE DATABASE IF NOT EXISTS track_service
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE track_service;

CREATE TABLE IF NOT EXISTS tracking_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tracking_number VARCHAR(64) NOT NULL UNIQUE,
    carrier_code VARCHAR(64),
    reference_number VARCHAR(64),
    latest_status VARCHAR(128),
    status_detail VARCHAR(512),
    last_event_time DATETIME(3) NULL,
    last_synced_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_tracking_records_last_synced (last_synced_at),
    INDEX idx_tracking_records_last_event (last_event_time),
    INDEX idx_tracking_records_refresh_state (last_synced_at, last_event_time),
    INDEX idx_tracking_records_carrier (carrier_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tracking_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    record_id BIGINT UNSIGNED NOT NULL,
    event_time DATETIME(3) NOT NULL,
    location VARCHAR(128),
    description VARCHAR(512),
    status VARCHAR(128),
    PRIMARY KEY (id),
    INDEX idx_tracking_events_record_time (record_id, event_time),
    CONSTRAINT fk_tracking_events_record
        FOREIGN KEY (record_id)
        REFERENCES tracking_records (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
