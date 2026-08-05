package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import db.postmigration.V104__employee_cell_mapping_support;
import db.postmigration.V105__cell_reporting_authority_mapping_support;

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
        assertThat(employeeCellMigrationIndex).isGreaterThanOrEqualTo(0);
        assertThat(cellAuthorityMigrationIndex).isGreaterThan(employeeCellMigrationIndex);
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
