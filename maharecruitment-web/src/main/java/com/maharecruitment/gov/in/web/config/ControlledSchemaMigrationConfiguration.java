package com.maharecruitment.gov.in.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Boot makes Hibernate wait for this initializer before validating the migrated schema. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.post-schema-flyway.before-hibernate", havingValue = "true")
@Import(DatabaseInitializationDependencyConfigurer.class)
public class ControlledSchemaMigrationConfiguration {
    @Bean
    FlywayMigrationInitializer controlledSchemaMigrationInitializer(PostSchemaFlywayRunner runner) {
        return new FlywayMigrationInitializer(runner.createFlyway(), runner::migrate);
    }
}
