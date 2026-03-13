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

CREATE TABLE agency_global_rank (
    agency_global_rank_id BIGINT NOT NULL,
    agency_id BIGINT NOT NULL,
    rank_number INTEGER NOT NULL,
    assigned_date TIMESTAMP NOT NULL,
    created_date_time TIMESTAMP NOT NULL,
    updated_date_time TIMESTAMP NOT NULL,
    CONSTRAINT pk_agency_global_rank
        PRIMARY KEY (agency_global_rank_id),
    CONSTRAINT fk_agency_global_rank_agency
        FOREIGN KEY (agency_id)
        REFERENCES agency_master (agency_id),
    CONSTRAINT uk_agency_global_rank_agency
        UNIQUE (agency_id)
);

CREATE INDEX idx_agency_global_rank_agency
    ON agency_global_rank (agency_id);

CREATE INDEX idx_agency_global_rank_rank
    ON agency_global_rank (rank_number);

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

-- Agency Candidate Submission and Interview Tracking Table
CREATE TABLE recruitment_interview_detail (
    recruitment_interview_detail_id BIGINT NOT NULL,
    recruitment_notification_id BIGINT NOT NULL,
    agency_id BIGINT NOT NULL,
    recruitment_designation_vacancy_id BIGINT NOT NULL,
    agency_user_id BIGINT NOT NULL,
    candidate_name VARCHAR(150) NOT NULL,
    candidate_email VARCHAR(255) NOT NULL,
    candidate_mobile VARCHAR(15) NOT NULL,
    candidate_education VARCHAR(255) NOT NULL,
    total_experience NUMERIC(5,1) NOT NULL,
    relevant_experience NUMERIC(5,1) NOT NULL,
    joining_time VARCHAR(50) NOT NULL,
    resume_original_name VARCHAR(255) NOT NULL,
    resume_file_path VARCHAR(700) NOT NULL,
    resume_file_type VARCHAR(120),
    resume_file_size BIGINT,
    candidate_status VARCHAR(50) NOT NULL,
    department_shortlisted_at TIMESTAMP,
    department_shortlisted_by_user_id BIGINT,
    department_shortlist_remarks VARCHAR(1000),
    interview_scheduled_at TIMESTAMP,
    interview_scheduled_by_user_id BIGINT,
    interview_date_time TIMESTAMP,
    interview_time_slot VARCHAR(100),
    interview_link VARCHAR(700),
    interview_remarks VARCHAR(1000),
    is_active BOOLEAN NOT NULL,
    created_date_time TIMESTAMP NOT NULL,
    updated_date_time TIMESTAMP NOT NULL,
    CONSTRAINT pk_recruitment_interview_detail
        PRIMARY KEY (recruitment_interview_detail_id),
    CONSTRAINT fk_recruitment_interview_notification
        FOREIGN KEY (recruitment_notification_id)
        REFERENCES recruitment_notification (recruitment_notification_id),
    CONSTRAINT fk_recruitment_interview_agency
        FOREIGN KEY (agency_id)
        REFERENCES agency_master (agency_id),
    CONSTRAINT fk_recruitment_interview_vacancy
        FOREIGN KEY (recruitment_designation_vacancy_id)
        REFERENCES recruitment_designation_vacancy (recruitment_designation_vacancy_id),
    CONSTRAINT uk_recruitment_interview_notification_agency_email
        UNIQUE (recruitment_notification_id, agency_id, candidate_email),
    CONSTRAINT uk_recruitment_interview_notification_agency_mobile
        UNIQUE (recruitment_notification_id, agency_id, candidate_mobile)
);

CREATE INDEX idx_recruitment_interview_notification
    ON recruitment_interview_detail (recruitment_notification_id);

CREATE INDEX idx_recruitment_interview_agency
    ON recruitment_interview_detail (agency_id);

CREATE INDEX idx_recruitment_interview_vacancy
    ON recruitment_interview_detail (recruitment_designation_vacancy_id);

CREATE INDEX idx_recruitment_interview_status
    ON recruitment_interview_detail (candidate_status);
