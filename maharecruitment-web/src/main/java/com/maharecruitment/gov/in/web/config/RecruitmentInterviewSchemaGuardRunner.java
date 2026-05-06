package com.maharecruitment.gov.in.web.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.recruitment.interview.schema-guard.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RecruitmentInterviewSchemaGuardRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecruitmentInterviewSchemaGuardRunner.class);
    private static final String TABLE_NAME = "recruitment_interview_detail";
    private static final String EMAIL_CONSTRAINT = "uk_recruitment_interview_notification_agency_email";
    private static final String MOBILE_CONSTRAINT = "uk_recruitment_interview_notification_agency_mobile";
    private static final String ACTIVE_EMAIL_INDEX = "idx_recruitment_interview_active_email_unique";
    private static final String ACTIVE_MOBILE_INDEX = "idx_recruitment_interview_active_mobile_unique";

    private final DataSource dataSource;

    public RecruitmentInterviewSchemaGuardRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureRecruitmentInterviewIndexes() {
        try (Connection connection = dataSource.getConnection()) {
            if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("alter table " + TABLE_NAME + " drop constraint if exists " + EMAIL_CONSTRAINT);
                statement.execute("alter table " + TABLE_NAME + " drop constraint if exists " + MOBILE_CONSTRAINT);
                statement.execute("drop index if exists " + ACTIVE_EMAIL_INDEX);
                statement.execute("drop index if exists " + ACTIVE_MOBILE_INDEX);
                statement.execute(
                        "create unique index if not exists " + ACTIVE_EMAIL_INDEX
                                + " on " + TABLE_NAME
                                + " (recruitment_notification_id, agency_id, candidate_email)"
                                + " where is_active = true");
                statement.execute(
                        "create unique index if not exists " + ACTIVE_MOBILE_INDEX
                                + " on " + TABLE_NAME
                                + " (recruitment_notification_id, agency_id, candidate_mobile)"
                                + " where is_active = true");
            }

            LOGGER.info("Recruitment interview schema guard applied successfully for table '{}'", TABLE_NAME);
        } catch (Exception ex) {
            LOGGER.warn("Recruitment interview schema guard failed: {}", ex.getMessage(), ex);
        }
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getTables(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
