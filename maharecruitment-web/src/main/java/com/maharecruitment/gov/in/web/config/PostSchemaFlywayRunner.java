package com.maharecruitment.gov.in.web.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PostSchemaFlywayRunner {

    private final DataSource dataSource;

    public PostSchemaFlywayRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .table("flyway_post_schema_history")
                .locations("classpath:db/postmigration")
                .load()
                .migrate();
    }
}
