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
import db.postmigration.V10__auth_department_attendance_reports_menu_backfill;
import db.postmigration.V11__pre_onboarding_interview_detail_nullable;
import db.postmigration.V12__auth_hr_internal_vacancy_menu_backfill;
import db.postmigration.V13__auth_complete_menu_seed_backfill;
import db.postmigration.V14__auth_hr_employees_menu_backfill;
import db.postmigration.V15__auth_hr_employees_menu_conflict_fix;
import db.postmigration.V16__project_scope_type_backfill;
import db.postmigration.V17__internal_vacancy_draft_status_support;
import db.postmigration.V18__recruitment_notification_internal_vacancy_support;
import db.postmigration.V19__auth_hr_all_candidate_details_menu_backfill;
import db.postmigration.V20__internal_vacancy_interview_panel_support;
import db.postmigration.V21__internal_vacancy_interview_authority_support;
import db.postmigration.V22__auth_role_name_normalization;

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
                .ignoreMigrationPatterns("*:missing")
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
                        new V8__auth_agency_profile_menu_backfill(),
                        new V10__auth_department_attendance_reports_menu_backfill(),
                        new V11__pre_onboarding_interview_detail_nullable(),
                        new V12__auth_hr_internal_vacancy_menu_backfill(),
                        new V13__auth_complete_menu_seed_backfill(),
                        new V14__auth_hr_employees_menu_backfill(),
                        new V15__auth_hr_employees_menu_conflict_fix(),
                        new V16__project_scope_type_backfill(),
                        new V17__internal_vacancy_draft_status_support(),
                        new V18__recruitment_notification_internal_vacancy_support(),
                        new V19__auth_hr_all_candidate_details_menu_backfill(),
                        new V20__internal_vacancy_interview_panel_support(),
                        new V21__internal_vacancy_interview_authority_support(),
                        new V22__auth_role_name_normalization())
                .load()
                .migrate();
        LOGGER.info("Post-schema Flyway migrations completed");
    }
}
