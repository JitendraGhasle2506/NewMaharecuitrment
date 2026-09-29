package com.maharecruitment.gov.in.web.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final Map<String, String> COLUMN_DEFINITIONS = Map.ofEntries(
            Map.entry("hr_onboarding_date", "date"),
            Map.entry("hr_onboarding_location", "varchar(255)"),
            Map.entry("hr_verified", "boolean not null default false"),
            Map.entry("hr_user_id", "bigint"),
            Map.entry("onboarded_at", "timestamp"),
            Map.entry("gender", "varchar(20)"),
            Map.entry("blood_group", "varchar(20)"),
            Map.entry("emergency_contact_name", "varchar(100)"),
            Map.entry("emergency_contact_relation", "varchar(50)"),
            Map.entry("emergency_contact_mobile", "varchar(15)"),
            Map.entry("emergency_contact_alt_mobile", "varchar(15)"),
            Map.entry("company_payroll_more_than_three_months", "boolean not null default false"),
            Map.entry("company_payroll_proof_original_name", "varchar(255)"),
            Map.entry("company_payroll_proof_file_path", "varchar(700)"),
            Map.entry("company_payroll_proof_file_type", "varchar(120)"),
            Map.entry("company_payroll_proof_file_size", "bigint"));
    private static final Set<String> EMPLOYEE_COLUMNS = Set.of("gender", "blood_group", "emergency_contact_name",
            "emergency_contact_relation", "emergency_contact_mobile", "emergency_contact_alt_mobile",
            "company_payroll_more_than_three_months");
    private final DataSource dataSource;

    public PreOnboardingSchemaGuardRunner(DataSource dataSource) { this.dataSource = dataSource; }

    @EventListener(ApplicationReadyEvent.class)
    public void ensurePreOnboardingColumns() {
        try (Connection connection = new MigrationLockTimeoutDataSource(dataSource).getConnection()) {
            if (apply(connection)) LOGGER.info("Pre-onboarding schema repair completed.");
        } catch (Exception ex) {
            LOGGER.warn("Pre-onboarding schema guard failed: {}", ex.getMessage(), ex);
        }
    }

    public static boolean apply(Connection connection) throws SQLException {
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) return false;
        Map<String, Boolean> preColumns = columns(connection, TABLE_NAME);
        if (preColumns.isEmpty()) return false;
        Map<String, Boolean> employeeColumns = columns(connection, "employee_master");
        List<String> changes = new ArrayList<>();
        COLUMN_DEFINITIONS.forEach((name, definition) -> {
            if (!preColumns.containsKey(name)) {
                changes.add("alter table " + TABLE_NAME + " add column if not exists " + name + " " + definition);
            }
            if (!employeeColumns.isEmpty() && EMPLOYEE_COLUMNS.contains(name) && !employeeColumns.containsKey(name)) {
                changes.add("alter table employee_master add column if not exists " + name + " " + definition);
            }
        });
        if (Boolean.FALSE.equals(preColumns.get("submitted_at"))) {
            changes.add("update " + TABLE_NAME + " set submitted_at = now() where submitted_at is null");
            changes.add("alter table " + TABLE_NAME + " alter column submitted_at set not null");
        }
        if (Boolean.TRUE.equals(preColumns.get("onboarding_date"))) {
            changes.add("alter table " + TABLE_NAME + " alter column onboarding_date drop not null");
        }
        if (changes.isEmpty()) return false;
        boolean ownsTransaction = connection.getAutoCommit();
        boolean safeToResetAutoCommit = ownsTransaction;
        if (ownsTransaction) connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.execute("set local lock_timeout = '5s'");
            for (String sql : changes) statement.execute(sql);
            if (ownsTransaction) connection.commit();
            return true;
        } catch (SQLException | RuntimeException failure) {
            if (ownsTransaction) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) {
                    safeToResetAutoCommit = false;
                    failure.addSuppressed(rollbackFailure);
                    try { connection.abort(Runnable::run); } catch (SQLException abortFailure) { failure.addSuppressed(abortFailure); }
                }
            }
            throw failure;
        } finally {
            if (safeToResetAutoCommit && !connection.isClosed()) connection.setAutoCommit(true);
        }
    }

    private static Map<String, Boolean> columns(Connection connection, String table) throws SQLException {
        Map<String, Boolean> columns = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select a.attname, a.attnotnull from pg_catalog.pg_class c
                join pg_catalog.pg_attribute a on a.attrelid = c.oid
                where c.relname = ? and pg_catalog.pg_table_is_visible(c.oid)
                  and c.relkind in ('r', 'p') and a.attnum > 0 and not a.attisdropped
                """)) {
            statement.setQueryTimeout(5);
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) columns.put(rows.getString(1), rows.getBoolean(2));
            }
        }
        return columns;
    }
}
