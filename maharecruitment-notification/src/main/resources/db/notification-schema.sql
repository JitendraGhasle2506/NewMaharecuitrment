CREATE TABLE IF NOT EXISTS notification_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    reference_id BIGINT,
    module VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_recipient (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES notification_event(id),
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    is_seen BOOLEAN NOT NULL DEFAULT FALSE,
    action_required BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_user_id
    ON notification_recipient(user_id);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_user_status
    ON notification_recipient(user_id, status);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_event_id
    ON notification_recipient(event_id);
