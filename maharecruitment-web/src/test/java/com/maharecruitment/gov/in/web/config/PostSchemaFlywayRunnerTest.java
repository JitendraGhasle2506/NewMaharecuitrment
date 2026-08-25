package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import db.postmigration.V104__employee_cell_mapping_support;
import db.postmigration.V105__cell_reporting_authority_mapping_support;
import db.postmigration.V106__project_master_department_mapping;
import db.postmigration.V107__otp_rate_limit_bucket_support;
import db.postmigration.V108__password_reset_otp_lock_support;
import db.postmigration.V109__auth_user_password_change_required;
import db.postmigration.V113__master_location_department_name_support;
import db.postmigration.V114__employee_profile_insert_compatibility;
import db.postmigration.V115__designation_rates_single_l1_per_period;
import db.postmigration.V116__designation_rates_2026_l1_defaults;
import db.postmigration.V117__internal_vacancy_hiring_request_details;
import db.postmigration.V118__internal_vacancy_replacement_employee_mapping;
import db.postmigration.V119__internal_vacancy_multiple_replacement_employees;

class PostSchemaFlywayRunnerTest {

    @Test
    void registersCellAuthorityMigrationAfterEmployeeCellMappingSupport() {
        PostSchemaFlywayRunner runner = new PostSchemaFlywayRunner(mock(DataSource.class));

        Object configuredMigrations = ReflectionTestUtils.invokeMethod(runner, "migrations");

        assertThat(configuredMigrations).isInstanceOf(List.class);
        List<?> migrations = (List<?>) configuredMigrations;
        assertThat(migrations).isNotEmpty();
        int employeeCellMigrationIndex = indexOf(migrations, V104__employee_cell_mapping_support.class);
        int cellAuthorityMigrationIndex = indexOf(
                migrations, V105__cell_reporting_authority_mapping_support.class);
        int projectDepartmentMigrationIndex = indexOf(
                migrations, V106__project_master_department_mapping.class);
        int otpRateLimitMigrationIndex = indexOf(migrations, V107__otp_rate_limit_bucket_support.class);
        int passwordResetLockMigrationIndex = indexOf(
                migrations, V108__password_reset_otp_lock_support.class);
        int requiredPasswordChangeMigrationIndex = indexOf(
                migrations, V109__auth_user_password_change_required.class);
        int locationDepartmentMigrationIndex = indexOf(
                migrations, V113__master_location_department_name_support.class);
        int employeeProfileCompatibilityMigrationIndex = indexOf(
                migrations, V114__employee_profile_insert_compatibility.class);
        int designationRateNormalizationMigrationIndex = indexOf(
                migrations, V115__designation_rates_single_l1_per_period.class);
        int designationRateDefaultsMigrationIndex = indexOf(
                migrations, V116__designation_rates_2026_l1_defaults.class);
        int internalVacancyHiringDetailsMigrationIndex = indexOf(
                migrations, V117__internal_vacancy_hiring_request_details.class);
        int replacementEmployeeMappingMigrationIndex = indexOf(
                migrations, V118__internal_vacancy_replacement_employee_mapping.class);
        int multipleReplacementEmployeesMigrationIndex = indexOf(
                migrations, V119__internal_vacancy_multiple_replacement_employees.class);
        assertThat(employeeCellMigrationIndex).isGreaterThanOrEqualTo(0);
        assertThat(cellAuthorityMigrationIndex).isGreaterThan(employeeCellMigrationIndex);
        assertThat(projectDepartmentMigrationIndex).isGreaterThan(cellAuthorityMigrationIndex);
        assertThat(otpRateLimitMigrationIndex).isGreaterThan(projectDepartmentMigrationIndex);
        assertThat(passwordResetLockMigrationIndex).isGreaterThan(otpRateLimitMigrationIndex);
        assertThat(requiredPasswordChangeMigrationIndex).isGreaterThan(passwordResetLockMigrationIndex);
        assertThat(locationDepartmentMigrationIndex).isGreaterThan(requiredPasswordChangeMigrationIndex);
        assertThat(employeeProfileCompatibilityMigrationIndex).isGreaterThan(locationDepartmentMigrationIndex);
        assertThat(designationRateNormalizationMigrationIndex)
                .isGreaterThan(employeeProfileCompatibilityMigrationIndex);
        assertThat(designationRateDefaultsMigrationIndex)
                .isGreaterThan(designationRateNormalizationMigrationIndex);
        assertThat(internalVacancyHiringDetailsMigrationIndex)
                .isGreaterThan(designationRateDefaultsMigrationIndex);
        assertThat(replacementEmployeeMappingMigrationIndex)
                .isGreaterThan(internalVacancyHiringDetailsMigrationIndex);
        assertThat(multipleReplacementEmployeesMigrationIndex)
                .isGreaterThan(replacementEmployeeMappingMigrationIndex);
    }

    private int indexOf(List<?> migrations, Class<?> migrationType) {
        for (int index = 0; index < migrations.size(); index++) {
            if (migrationType.isInstance(migrations.get(index))) {
                return index;
            }
        }
        return -1;
    }
}
