package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V34__recruitment_internal_level_two_forwarded_status_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)
                || !tableExists(connection, "recruitment_internal_level_two_schedule")
                || !columnExists(connection, "recruitment_internal_level_two_schedule", "l2_workflow_status")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute(
                "update recruitment_internal_level_two_schedule schedule "
                        + "set l2_workflow_status = 'L2_FORWARDED_TO_AGENCY' "
                        + "from recruitment_interview_detail candidate "
                        + "where candidate.recruitment_interview_detail_id = schedule.recruitment_interview_detail_id "
                        + "and upper(coalesce(candidate.final_decision_status, '')) in ('SELECTED', 'REJECTED') "
                        + "and upper(coalesce(schedule.l2_workflow_status, '')) in ('L2_SELECTED', 'L2_REJECTED')");
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
