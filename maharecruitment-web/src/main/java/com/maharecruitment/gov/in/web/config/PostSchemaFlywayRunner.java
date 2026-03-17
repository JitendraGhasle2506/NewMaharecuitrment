package com.maharecruitment.gov.in.web.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import db.postmigration.R__auth_reference_data;
import db.postmigration.R__department_and_recruitment_reference_data;
import db.postmigration.R__department_and_recruitment_schema;
import db.postmigration.R__master_reference_data;
import db.postmigration.V2__auth_menu_seed_fix;
import db.postmigration.V3__auth_menu_seed_backfill;
import db.postmigration.V4__auth_hr_resigned_menu_backfill;
import db.postmigration.V5__auth_agency_resigned_menu_backfill;
import db.postmigration.V6__auth_agency_resignation_menu_fix;
import db.postmigration.V7__recruitment_pre_onboarding_hr_columns_fix;
import db.postmigration.V8__auth_agency_profile_menu_backfill;

@Component
@ConditionalOnClass(name = "org.flywaydb.core.Flyway")
@ConditionalOnProperty(name = "app.post-schema-flyway.enabled", havingValue = "true", matchIfMissing = true)
public class PostSchemaFlywayRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostSchemaFlywayRunner.class);

    private final DataSource dataSource;

    public PostSchemaFlywayRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        LOGGER.info("Running post-schema Flyway migrations");
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .table("flyway_post_schema_history")
                .javaMigrations(
                        new R__department_and_recruitment_schema(),
                        new R__master_reference_data(),
                        new R__department_and_recruitment_reference_data(),
                        new R__auth_reference_data(),
                        new V2__auth_menu_seed_fix(),
                        new V3__auth_menu_seed_backfill(),
                        new V4__auth_hr_resigned_menu_backfill(),
                        new V5__auth_agency_resigned_menu_backfill(),
                        new V6__auth_agency_resignation_menu_fix(),
                        new V7__recruitment_pre_onboarding_hr_columns_fix(),
                        new V8__auth_agency_profile_menu_backfill())
                .load()
                .migrate();
        LOGGER.info("Post-schema Flyway migrations completed");
    }
}
