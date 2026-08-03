package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import db.postmigration.V102__employee_location_mapping_primary_support;

class PostSchemaFlywayRunnerTest {

    @Test
    void registersEmployeeLocationPrimaryMigrationAfterEarlierVersions() {
        PostSchemaFlywayRunner runner = new PostSchemaFlywayRunner(mock(DataSource.class));

        Object configuredMigrations = ReflectionTestUtils.invokeMethod(runner, "migrations");

        assertThat(configuredMigrations).isInstanceOf(List.class);
        List<?> migrations = (List<?>) configuredMigrations;
        assertThat(migrations).isNotEmpty();
        assertThat(migrations.get(migrations.size() - 1))
                .isInstanceOf(V102__employee_location_mapping_primary_support.class);
    }
}
