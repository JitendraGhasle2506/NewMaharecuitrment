package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ControlledSchemaMigrationConfigurationTest {
    @Test
    void completesMigrationsBeforeDatabaseDependentBeansInitialize() {
        PostSchemaFlywayRunner runner = mock(PostSchemaFlywayRunner.class);
        Flyway flyway = mock(Flyway.class);
        when(runner.createFlyway()).thenReturn(flyway);
        new ApplicationContextRunner()
                .withUserConfiguration(ControlledSchemaMigrationConfiguration.class, DatabaseConsumerConfiguration.class)
                .withBean(PostSchemaFlywayRunner.class, () -> runner)
                .withPropertyValues("app.post-schema-flyway.before-hibernate=true")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(FlywayMigrationInitializer.class);
                    assertThat(context.getBean(DatabaseConsumer.class)).isNotNull();
                    verify(runner, times(1)).migrate(flyway);
                });
    }

    @Test
    void leavesLegacyStartupOrderAloneUnlessValidationModeIsEnabled() {
        new ApplicationContextRunner().withUserConfiguration(ControlledSchemaMigrationConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(FlywayMigrationInitializer.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class DatabaseConsumerConfiguration {
        @Bean
        @DependsOnDatabaseInitialization
        DatabaseConsumer databaseConsumer(PostSchemaFlywayRunner runner) {
            verify(runner).migrate(any(Flyway.class));
            return new DatabaseConsumer();
        }
    }
    static class DatabaseConsumer { }
}
