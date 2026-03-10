package com.maharecruitment.gov.in.department.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationActivityType;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;

@Component
public class DepartmentStatusConstraintInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DepartmentStatusConstraintInitializer.class);

    private static final List<String> LEGACY_STATUS_VALUES = List.of(
            // Legacy status values already present in existing databases.
            "DRAFT",
            "SUBMITTED",
            "UNDER_REVIEW",
            "APPROVED",
            "REJECTED",
            // Current workflow status values.
            "SUBMITTED_TO_HR",
            "HR_SENT_BACK",
            "CORRECTED_BY_DEPARTMENT",
            "HR_APPROVED",
            "HR_REJECTED",
            "AUDITOR_REVIEW",
            "AUDITOR_SENT_BACK",
            "AUDITOR_APPROVED",
            "COMPLETED");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DepartmentStatusConstraintInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql()) {
            log.info("Department status constraint bootstrap skipped for non-PostgreSQL datasource.");
            return;
        }

        updateConstraint(
                "department_project_application",
                "application_status",
                "department_project_application_application_status_check",
                allowedStatusValues());

        updateConstraint(
                "department_project_application_activity",
                "previous_status",
                "department_project_application_activity_previous_status_check",
                allowedStatusValues());

        updateConstraint(
                "department_project_application_activity",
                "new_status",
                "department_project_application_activity_new_status_check",
                allowedStatusValues());

        updateConstraint(
                "department_project_application_activity",
                "activity_type",
                "department_project_application_activity_activity_type_check",
                allowedActivityTypes());
    }

    private List<String> allowedStatusValues() {
        return Stream.concat(
                Stream.of(DepartmentApplicationStatus.values()).map(Enum::name),
                LEGACY_STATUS_VALUES.stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> allowedActivityTypes() {
        return Stream.of(DepartmentApplicationActivityType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void updateConstraint(
            String tableName,
            String columnName,
            String constraintName,
            List<String> desiredAllowedValues) {
        List<String> effectiveAllowedValues = buildEffectiveAllowedValues(tableName, columnName, desiredAllowedValues);

        String allowedValuesSql = effectiveAllowedValues.stream()
                .map(value -> "'" + escapeSqlLiteral(value) + "'")
                .collect(Collectors.joining(", "));

        String dropSql = "ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + constraintName;
        String addSql = "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName
                + " CHECK (" + columnName + " IN (" + allowedValuesSql + "))";

        try {
            jdbcTemplate.execute(dropSql);
            jdbcTemplate.execute(addSql);
            log.info("Constraint ensured: {}.{} -> {}", tableName, columnName, constraintName);
        } catch (DataAccessException ex) {
            log.error("Unable to ensure constraint {} on table {}.", constraintName, tableName, ex);
            throw ex;
        }
    }

    private List<String> buildEffectiveAllowedValues(
            String tableName,
            String columnName,
            List<String> desiredAllowedValues) {
        Set<String> mergedValues = new LinkedHashSet<>();

        for (String value : desiredAllowedValues) {
            if (value != null && !value.isBlank()) {
                mergedValues.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }

        for (String existingValue : fetchExistingValues(tableName, columnName)) {
            if (existingValue != null && !existingValue.isBlank()) {
                mergedValues.add(existingValue.trim().toUpperCase(Locale.ROOT));
            }
        }

        return List.copyOf(mergedValues);
    }

    private List<String> fetchExistingValues(String tableName, String columnName) {
        String sql = "SELECT DISTINCT " + columnName
                + " FROM " + tableName
                + " WHERE " + columnName + " IS NOT NULL AND TRIM(" + columnName + ") <> ''";
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (DataAccessException ex) {
            log.warn("Unable to fetch existing values for {}.{} while preparing constraint.", tableName, columnName, ex);
            return List.of();
        }
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String databaseProductName = metadata.getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase().contains("postgresql");
        } catch (Exception ex) {
            log.warn("Unable to determine database product name for constraint bootstrap.", ex);
            return false;
        }
    }
}
