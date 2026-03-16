package com.maharecruitment.gov.in.web.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
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
                .javaMigrations(
                        new R__department_and_recruitment_schema(),
                        new R__master_reference_data(),
                        new R__department_and_recruitment_reference_data(),
                        new R__auth_reference_data(),
                        new V2__auth_menu_seed_fix(),
                        new V3__auth_menu_seed_backfill(),
                        new V4__auth_hr_resigned_menu_backfill(),
                        new V5__auth_agency_resigned_menu_backfill(),
                        new V6__auth_agency_resignation_menu_fix())
                .load()
                .migrate();
    }
}
