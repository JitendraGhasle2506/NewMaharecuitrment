package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V33__recruitment_internal_level_two_workflow_status_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_internal_level_two_schedule")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "l2_workflow_status",
                "varchar(40)");

        dropNotNull(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "scheduled_by_user_id");
        dropNotNull(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "scheduled_at");
        dropNotNull(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "interview_date_time");
        dropNotNull(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "interview_time_slot");

        jdbcTemplate.execute(
                "insert into recruitment_internal_level_two_schedule ("
                        + "recruitment_interview_detail_id, "
                        + "scheduled_by_user_id, "
                        + "scheduled_at, "
                        + "interview_date_time, "
                        + "interview_time_slot, "
                        + "meeting_link, "
                        + "remarks, "
                        + "panel_assigned_by_user_id, "
                        + "panel_assigned_at, "
                        + "hr_time_change_requested, "
                        + "hr_time_change_reason, "
                        + "hr_time_change_requested_at, "
                        + "hr_time_change_requested_by_user_id, "
                        + "l2_workflow_status, "
                        + "created_date_time, "
                        + "updated_date_time"
                        + ") "
                        + "select "
                        + "candidate.recruitment_interview_detail_id, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "false, "
                        + "null, "
                        + "null, "
                        + "null, "
                        + "'READY_FOR_L2', "
                        + "coalesce(candidate.assessment_submitted_at, candidate.updated_date_time, "
                        + "candidate.created_date_time, current_timestamp), "
                        + "current_timestamp "
                        + "from recruitment_interview_detail candidate "
                        + "join recruitment_notification notification "
                        + "on notification.recruitment_notification_id = candidate.recruitment_notification_id "
                        + "join recruitment_assessment_feedback assessment "
                        + "on assessment.recruitment_interview_detail_id = candidate.recruitment_interview_detail_id "
                        + "where notification.internal_vacancy_opening_id is not null "
                        + "and candidate.is_active = true "
                        + "and coalesce(candidate.assessment_submitted, false) = true "
                        + "and upper(coalesce(assessment.recommendation_status, '')) = 'RECOMMENDED' "
                        + "and candidate.final_decision_status is null "
                        + "and not exists ("
                        + "select 1 from recruitment_internal_level_two_schedule schedule "
                        + "where schedule.recruitment_interview_detail_id = candidate.recruitment_interview_detail_id"
                        + ")");

        jdbcTemplate.execute(
                "update recruitment_internal_level_two_schedule schedule "
                        + "set l2_workflow_status = case "
                        + "when upper(coalesce(candidate.final_decision_status, '')) = 'SELECTED' "
                        + "then 'L2_SELECTED' "
                        + "when upper(coalesce(candidate.final_decision_status, '')) = 'REJECTED' "
                        + "then 'L2_REJECTED' "
                        + "when coalesce(schedule.hr_time_change_requested, false) = true "
                        + "then 'L2_RESCHEDULE_REQUESTED' "
                        + "when exists ("
                        + "select 1 from recruitment_internal_level_two_feedback feedback "
                        + "where feedback.recruitment_internal_level_two_schedule_id = "
                        + "schedule.recruitment_internal_level_two_schedule_id"
                        + ") then 'L2_UNDER_HR_REVIEW' "
                        + "when schedule.panel_assigned_at is not null then 'L2_PANEL_ASSIGNED' "
                        + "when schedule.interview_date_time is not null then 'L2_SCHEDULED' "
                        + "else 'READY_FOR_L2' "
                        + "end "
                        + "from recruitment_interview_detail candidate "
                        + "where candidate.recruitment_interview_detail_id = "
                        + "schedule.recruitment_interview_detail_id");

        jdbcTemplate.execute(
                "alter table recruitment_internal_level_two_schedule "
                        + "alter column l2_workflow_status set not null");
    }

    private void addColumnIfMissing(
            JdbcTemplate jdbcTemplate,
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition) {
        if (!columnExists(connection, tableName, columnName)) {
            jdbcTemplate.execute(
                    "alter table " + tableName + " add column " + columnName + " " + columnDefinition);
        }
    }

    private void dropNotNull(
            JdbcTemplate jdbcTemplate,
            Connection connection,
            String tableName,
            String columnName) {
        if (columnExists(connection, tableName, columnName)) {
            jdbcTemplate.execute(
                    "alter table " + tableName + " alter column " + columnName + " drop not null");
        }
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getColumns(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
