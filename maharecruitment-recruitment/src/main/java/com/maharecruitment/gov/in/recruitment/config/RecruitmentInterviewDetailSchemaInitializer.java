package com.maharecruitment.gov.in.recruitment.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(55)
public class RecruitmentInterviewDetailSchemaInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentInterviewDetailSchemaInitializer.class);

    private static final String TABLE_NAME = "recruitment_interview_detail";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public RecruitmentInterviewDetailSchemaInitializer(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isPostgreSql()) {
            log.info("Recruitment interview schema bootstrap skipped for non-PostgreSQL datasource.");
            return;
        }

        ensureColumn("department_interview_change_requested", "BOOLEAN DEFAULT FALSE");
        ensureColumn("department_interview_change_reason", "VARCHAR(1000)");
        ensureColumn("department_interview_change_requested_at", "TIMESTAMP");
        ensureColumn("department_interview_change_requested_by_user_id", "BIGINT");

        ensureColumn("assessment_submitted", "BOOLEAN DEFAULT FALSE");
        ensureColumn("assessment_submitted_at", "TIMESTAMP");
        ensureColumn("assessment_submitted_by_user_id", "BIGINT");

        ensureColumn("final_decision_status", "VARCHAR(20)");
        ensureColumn("final_decision_at", "TIMESTAMP");
        ensureColumn("final_decision_by_user_id", "BIGINT");
        ensureColumn("final_decision_remarks", "VARCHAR(1000)");

        backfillAssessmentSubmittedFlag();
        log.info("Recruitment interview schema bootstrap completed for table '{}'.", TABLE_NAME);
    }

    private void ensureColumn(String columnName, String columnDefinition) {
        try {
            if (columnExists(TABLE_NAME, columnName)) {
                return;
            }

            String alterSql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + columnDefinition;
            jdbcTemplate.execute(alterSql);
            log.info("Column added: {}.{} {}", TABLE_NAME, columnName, columnDefinition);
        } catch (Exception ex) {
            log.error("Unable to add missing column {}.{}.", TABLE_NAME, columnName, ex);
            throw ex;
        }
    }

    private void backfillAssessmentSubmittedFlag() {
        try {
            jdbcTemplate.execute(
                    "UPDATE " + TABLE_NAME + " "
                            + "SET assessment_submitted = CASE "
                            + "WHEN assessment_submitted_at IS NOT NULL THEN TRUE "
                            + "ELSE FALSE END "
                            + "WHERE assessment_submitted IS NULL");
            jdbcTemplate.execute(
                    "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN assessment_submitted SET DEFAULT FALSE");
            jdbcTemplate.execute(
                    "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN assessment_submitted SET NOT NULL");
        } catch (Exception ex) {
            log.error("Unable to backfill/normalize column {}.assessment_submitted.", TABLE_NAME, ex);
            throw ex;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();

            try (ResultSet columns = metaData.getColumns(null, schema, tableName, columnName)) {
                if (columns.next()) {
                    return true;
                }
            }

            // Fallback for databases returning uppercase metadata
            try (ResultSet columns = metaData.getColumns(
                    null,
                    schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT))) {
                return columns.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Unable to inspect metadata for " + tableName + "." + columnName,
                    ex);
        }
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception ex) {
            log.warn("Unable to detect database product for recruitment schema bootstrap.", ex);
            return false;
        }
    }
}

