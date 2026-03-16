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
@ConditionalOnProperty(name = "app.pre-onboarding.schema-guard.enabled", havingValue = "true", matchIfMissing = true)
public class PreOnboardingSchemaGuardRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreOnboardingSchemaGuardRunner.class);
    private static final String TABLE_NAME = "agency_candidate_pre_onboarding";

    private final DataSource dataSource;

    public PreOnboardingSchemaGuardRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensurePreOnboardingColumns() {
        try (Connection connection = dataSource.getConnection()) {
            if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "alter table " + TABLE_NAME + " add column if not exists hr_onboarding_date date");
                statement.execute(
                        "alter table " + TABLE_NAME + " add column if not exists hr_onboarding_location varchar(255)");
                statement.execute(
                        "alter table " + TABLE_NAME + " add column if not exists hr_verified boolean not null default false");
                statement.execute(
                        "alter table " + TABLE_NAME + " add column if not exists hr_user_id bigint");
                statement.execute(
                        "alter table " + TABLE_NAME + " add column if not exists onboarded_at timestamp");

                if (columnExists(connection, TABLE_NAME, "submitted_at")) {
                    statement.execute(
                            "update " + TABLE_NAME + " set submitted_at = now() where submitted_at is null");
                    statement.execute(
                            "alter table " + TABLE_NAME + " alter column submitted_at set not null");
                }
            }

            LOGGER.info("Pre-onboarding schema guard applied successfully for table '{}'", TABLE_NAME);
        } catch (Exception ex) {
            LOGGER.warn("Pre-onboarding schema guard failed: {}", ex.getMessage(), ex);
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

    private boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            String schema = connection.getSchema();
            try (ResultSet rs = connection.getMetaData().getColumns(
                    null,
                    schema,
                    tableName,
                    columnName)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = connection.getMetaData().getColumns(
                    null,
                    schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT))) {
                return rs.next();
            }
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
