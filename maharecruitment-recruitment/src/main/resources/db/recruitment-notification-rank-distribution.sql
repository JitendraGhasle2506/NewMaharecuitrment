-- Recruitment Notification Rank Distribution (ANSI SQL oriented)
-- Module: maharecruitment-recruitment
--
-- Existing tables used:
--   agency_master (existing)
--   recruitment_notification (existing)
--
-- Note for multi-database support:
--   Keep PK columns as BIGINT and apply vendor-specific identity/sequence strategy at deployment time.
--   This keeps table/constraint/query design ANSI-compatible across PostgreSQL, MySQL, Oracle, SQL Server.

CREATE TABLE recruitment_notification_agency_rank (
    recruitment_notification_agency_rank_id BIGINT NOT NULL,
    recruitment_notification_id BIGINT NOT NULL,
    agency_id BIGINT NOT NULL,
    rank_number INTEGER NOT NULL,
    assigned_date TIMESTAMP NOT NULL,
    created_date_time TIMESTAMP NOT NULL,
    updated_date_time TIMESTAMP NOT NULL,
    CONSTRAINT pk_recruitment_notification_agency_rank
        PRIMARY KEY (recruitment_notification_agency_rank_id),
    CONSTRAINT fk_rnar_notification
        FOREIGN KEY (recruitment_notification_id)
        REFERENCES recruitment_notification (recruitment_notification_id),
    CONSTRAINT fk_rnar_agency
        FOREIGN KEY (agency_id)
        REFERENCES agency_master (agency_id),
    CONSTRAINT uk_rnar_notification_agency
        UNIQUE (recruitment_notification_id, agency_id)
);

CREATE INDEX idx_rnar_notification
    ON recruitment_notification_agency_rank (recruitment_notification_id);

CREATE INDEX idx_rnar_agency
    ON recruitment_notification_agency_rank (agency_id);

CREATE INDEX idx_rnar_rank
    ON recruitment_notification_agency_rank (rank_number);

CREATE INDEX idx_rnar_notification_rank
    ON recruitment_notification_agency_rank (recruitment_notification_id, rank_number);

CREATE TABLE rank_release_rule (
    rank_release_rule_id BIGINT NOT NULL,
    rank_number INTEGER NOT NULL,
    release_after_days INTEGER NOT NULL,
    delay_from_previous_rank_days INTEGER NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    is_active BOOLEAN NOT NULL,
    created_date_time TIMESTAMP NOT NULL,
    updated_date_time TIMESTAMP NOT NULL,
    CONSTRAINT pk_rank_release_rule
        PRIMARY KEY (rank_release_rule_id),
    CONSTRAINT uk_rank_release_rule_rank
        UNIQUE (rank_number)
);

CREATE INDEX idx_rank_release_rule_rank
    ON rank_release_rule (rank_number);

CREATE TABLE agency_notification_tracking (
    agency_notification_tracking_id BIGINT NOT NULL,
    recruitment_notification_id BIGINT NOT NULL,
    agency_id BIGINT NOT NULL,
    released_rank INTEGER NOT NULL,
    notified_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    read_at TIMESTAMP NULL,
    responded_at TIMESTAMP NULL,
    created_date_time TIMESTAMP NOT NULL,
    updated_date_time TIMESTAMP NOT NULL,
    CONSTRAINT pk_agency_notification_tracking
        PRIMARY KEY (agency_notification_tracking_id),
    CONSTRAINT fk_ant_notification
        FOREIGN KEY (recruitment_notification_id)
        REFERENCES recruitment_notification (recruitment_notification_id),
    CONSTRAINT fk_ant_agency
        FOREIGN KEY (agency_id)
        REFERENCES agency_master (agency_id),
    CONSTRAINT uk_ant_notification_agency
        UNIQUE (recruitment_notification_id, agency_id)
);

CREATE INDEX idx_ant_notification
    ON agency_notification_tracking (recruitment_notification_id);

CREATE INDEX idx_ant_agency
    ON agency_notification_tracking (agency_id);

CREATE INDEX idx_ant_status
    ON agency_notification_tracking (status);

CREATE INDEX idx_ant_notification_rank
    ON agency_notification_tracking (recruitment_notification_id, released_rank);

-- Seed rank release rules (example):
-- Rank 1 -> immediate
-- Rank 2 -> after 3 days from rank 1
-- Rank 3 -> after 3 days from rank 2
INSERT INTO rank_release_rule (
    rank_release_rule_id, rank_number, release_after_days, delay_from_previous_rank_days, effective_from, effective_to, is_active, created_date_time, updated_date_time
)
VALUES (1, 1, 0, 0, CURRENT_DATE, DATE '9999-12-31', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rank_release_rule (
    rank_release_rule_id, rank_number, release_after_days, delay_from_previous_rank_days, effective_from, effective_to, is_active, created_date_time, updated_date_time
)
VALUES (2, 2, 3, 3, CURRENT_DATE, DATE '9999-12-31', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rank_release_rule (
    rank_release_rule_id, rank_number, release_after_days, delay_from_previous_rank_days, effective_from, effective_to, is_active, created_date_time, updated_date_time
)
VALUES (3, 3, 3, 3, CURRENT_DATE, DATE '9999-12-31', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Visibility query for a specific agency:
-- Returns only notifications released to that agency and not closed.
SELECT
    n.recruitment_notification_id,
    n.request_id,
    n.department_registration_id,
    n.department_project_application_id,
    n.project_id,
    t.released_rank,
    t.notified_at,
    t.status AS agency_tracking_status
FROM agency_notification_tracking t
INNER JOIN recruitment_notification n
    ON n.recruitment_notification_id = t.recruitment_notification_id
WHERE t.agency_id = :agencyId
  AND t.status IN ('RELEASED', 'READ', 'RESPONDED')
  AND n.status IN ('PENDING_ALLOCATION', 'IN_PROGRESS')
ORDER BY t.notified_at DESC;
