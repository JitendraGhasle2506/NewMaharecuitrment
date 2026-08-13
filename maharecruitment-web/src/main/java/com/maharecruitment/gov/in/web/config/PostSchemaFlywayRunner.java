package com.maharecruitment.gov.in.web.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import db.postmigration.R__auth_reference_data;
import db.postmigration.R__common_mahait_profile_schema;
import db.postmigration.R__department_and_recruitment_reference_data;
import db.postmigration.R__department_and_recruitment_schema;
import db.postmigration.R__hr_employee_cell_mapping_navigation;
import db.postmigration.R__master_reference_data;
import db.postmigration.V7__recruitment_pre_onboarding_hr_columns_fix;
import db.postmigration.V11__pre_onboarding_interview_detail_nullable;
import db.postmigration.V16__project_scope_type_backfill;
import db.postmigration.V17__internal_vacancy_draft_status_support;
import db.postmigration.V18__recruitment_notification_internal_vacancy_support;
import db.postmigration.V20__internal_vacancy_interview_panel_support;
import db.postmigration.V21__internal_vacancy_interview_authority_support;
import db.postmigration.V22__auth_role_name_normalization;
import db.postmigration.V24__recruitment_assessment_internal_vacancy_support;
import db.postmigration.V25__internal_feedback_leadership_quality_marks;
import db.postmigration.V26__recruitment_internal_level_two_schedule_support;
import db.postmigration.V28__recruitment_internal_level_two_hr_workflow_support;
import db.postmigration.V30__recruitment_internal_level_two_panel_user_support;
import db.postmigration.V31__recruitment_internal_level_two_feedback_support;
import db.postmigration.V33__recruitment_internal_level_two_workflow_status_support;
import db.postmigration.V39__mahait_profile_cin_number_support;
import db.postmigration.V40__department_tax_invoice_support;
import db.postmigration.V49__attendance_internal_daily_audit_columns_support;
import db.postmigration.V50__pre_onboarding_onboarding_date_optional;
import db.postmigration.V52__attendance_holiday_soft_delete_support;
import db.postmigration.V53__attendance_week_off_working_day_support;
import db.postmigration.V54__auth_user_soft_delete_support;
import db.postmigration.V55__department_approved_payment_report_index_support;
import db.postmigration.V56__master_cell_master_support;
import db.postmigration.V57__project_cell_mapping_support;
import db.postmigration.V58__project_soft_delete_support;
import db.postmigration.V59__attendance_leave_compoff_support;
import db.postmigration.V66__attendance_internal_daily_unique_employee_date;
import db.postmigration.V67__pre_onboarding_company_payroll_proof;
import db.postmigration.V68__employee_master_mahait_onboarding_date;
import db.postmigration.V69__employee_master_company_payroll_more_than_three_months;
import db.postmigration.V71__master_wing_cell_mapping_support;
import db.postmigration.V73__agency_monthly_bill_stale_period_constraint_cleanup;
import db.postmigration.V75__team_management_hierarchy_support;
import db.postmigration.V76__team_master_cell_mapping_primary;
import db.postmigration.V77__position_master_level_mapping;
import db.postmigration.V78__position_master_project_optional;
import db.postmigration.V79__position_master_cell_mapping;
import db.postmigration.V80__sensitive_identity_data_encryption;
import db.postmigration.V81__otp_verification_state_support;
import db.postmigration.V82__master_location_master_support;
import db.postmigration.V83__master_location_office_name_support;
import db.postmigration.V84__employee_location_mapping_support;
import db.postmigration.V85__employee_location_mapping_audit_log_support;
import db.postmigration.V86__master_location_radius_meters_support;
import db.postmigration.V87__mobile_attendance_internal_daily_support;
import db.postmigration.V88__pre_onboarding_photo_embedding_support;
import db.postmigration.V89__employee_profile_support;
import db.postmigration.V90__employee_master_embedded_support;
import db.postmigration.V91__employee_fcm_token_support;
import db.postmigration.V92__auth_employee_user_mapping_hardening;
import db.postmigration.V94__acl_sms_transaction_log_support;
import db.postmigration.V95__mobile_attendance_time_only_columns;
import db.postmigration.V96__attendance_source_status_flags;
import db.postmigration.V97__employee_mobile_photo_support;
import db.postmigration.V98__attendance_status_flags_from_source_columns;
import db.postmigration.V99__password_reset_request_support;
import db.postmigration.V100__agency_master_audit_details_text;
import db.postmigration.V101__employee_master_department_id_support;
import db.postmigration.V102__employee_location_mapping_primary_support;
import db.postmigration.V104__employee_cell_mapping_support;
import db.postmigration.V105__cell_reporting_authority_mapping_support;
import db.postmigration.V106__project_master_department_mapping;
import db.postmigration.V107__otp_rate_limit_bucket_support;
import db.postmigration.V108__password_reset_otp_lock_support;
import db.postmigration.V109__auth_user_password_change_required;
import db.postmigration.V110__login_logout_audit_history;

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
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .table("flyway_post_schema_history")
                .ignoreMigrationPatterns("*:missing")
                .javaMigrations(migrations().toArray(JavaMigration[]::new))
                .load();

        if (hasFailedPostSchemaMigration()) {
            LOGGER.warn(
                    "Detected failed entries in flyway_post_schema_history. Repairing the post-schema history before retrying.");
            flyway.repair();
        }

        try {
            flyway.migrate();
        } catch (org.flywaydb.core.api.exception.FlywayValidateException ex) {
            LOGGER.warn(
                    "Flyway validation failed (possibly due to deleted or modified migrations). Attempting repair and retrying migrate: {}",
                    ex.getMessage());
            flyway.repair();
            flyway.migrate();
        }
        LOGGER.info("Post-schema Flyway migrations completed");
    }

    private List<JavaMigration> migrations() {
        return List.of(
                new R__department_and_recruitment_schema(),
                new R__master_reference_data(),
                new R__department_and_recruitment_reference_data(),
                new R__auth_reference_data(),
                new R__common_mahait_profile_schema(),
                new V7__recruitment_pre_onboarding_hr_columns_fix(),
                new V11__pre_onboarding_interview_detail_nullable(),
                new V16__project_scope_type_backfill(),
                new V17__internal_vacancy_draft_status_support(),
                new V18__recruitment_notification_internal_vacancy_support(),
                new V20__internal_vacancy_interview_panel_support(),
                new V21__internal_vacancy_interview_authority_support(),
                new V22__auth_role_name_normalization(),
                new V24__recruitment_assessment_internal_vacancy_support(),
                new V25__internal_feedback_leadership_quality_marks(),
                new V26__recruitment_internal_level_two_schedule_support(),
                new V28__recruitment_internal_level_two_hr_workflow_support(),
                new V30__recruitment_internal_level_two_panel_user_support(),
                new V31__recruitment_internal_level_two_feedback_support(),
                new V33__recruitment_internal_level_two_workflow_status_support(),
                new V39__mahait_profile_cin_number_support(),
                new V40__department_tax_invoice_support(),
                new V49__attendance_internal_daily_audit_columns_support(),
                new V50__pre_onboarding_onboarding_date_optional(),
                new V52__attendance_holiday_soft_delete_support(),
                new V53__attendance_week_off_working_day_support(),
                new V54__auth_user_soft_delete_support(),
                new V55__department_approved_payment_report_index_support(),
                new V56__master_cell_master_support(),
                new V57__project_cell_mapping_support(),
                new V58__project_soft_delete_support(),
                new V59__attendance_leave_compoff_support(),
                new V66__attendance_internal_daily_unique_employee_date(),
                new V67__pre_onboarding_company_payroll_proof(),
                new V68__employee_master_mahait_onboarding_date(),
                new V69__employee_master_company_payroll_more_than_three_months(),
                new V71__master_wing_cell_mapping_support(),
                new V73__agency_monthly_bill_stale_period_constraint_cleanup(),
                new V75__team_management_hierarchy_support(),
                new V76__team_master_cell_mapping_primary(),
                new V77__position_master_level_mapping(),
                new V78__position_master_project_optional(),
                new V79__position_master_cell_mapping(),
                new V80__sensitive_identity_data_encryption(),
                new V81__otp_verification_state_support(),
                new V82__master_location_master_support(),
                new V83__master_location_office_name_support(),
                new V84__employee_location_mapping_support(),
                new V85__employee_location_mapping_audit_log_support(),
                new V86__master_location_radius_meters_support(),
                new V87__mobile_attendance_internal_daily_support(),
                new V88__pre_onboarding_photo_embedding_support(),
                new V89__employee_profile_support(),
                new V90__employee_master_embedded_support(),
                new V91__employee_fcm_token_support(),
                new V92__auth_employee_user_mapping_hardening(),
                new V94__acl_sms_transaction_log_support(),
                new V95__mobile_attendance_time_only_columns(),
                new V96__attendance_source_status_flags(),
                new V97__employee_mobile_photo_support(),
                new V98__attendance_status_flags_from_source_columns(),
                new V99__password_reset_request_support(),
                new V100__agency_master_audit_details_text(),
                new V101__employee_master_department_id_support(),
                new V102__employee_location_mapping_primary_support(),
                new V104__employee_cell_mapping_support(),
                new V105__cell_reporting_authority_mapping_support(),
                new V106__project_master_department_mapping(),
                new V107__otp_rate_limit_bucket_support(),
                new V108__password_reset_otp_lock_support(),
                new V109__auth_user_password_change_required(),
                new V110__login_logout_audit_history(),
                new R__hr_employee_cell_mapping_navigation());
    }

    private boolean hasFailedPostSchemaMigration() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select 1 from flyway_post_schema_history where success = false limit 1")) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }
}
