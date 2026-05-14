package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V34__recruitment_assessment_feedback_employee_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_assessment_feedback")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        // Make interviewer_user_id nullable to support employee assessors
        if (columnExists(connection, "recruitment_assessment_feedback", "interviewer_user_id")) {
            jdbcTemplate.execute(
                    "alter table recruitment_assessment_feedback alter column interviewer_user_id drop not null");
        }

        // Add interviewer_employee_id column if it doesn't exist
        if (!columnExists(connection, "recruitment_assessment_feedback", "interviewer_employee_id")) {
            jdbcTemplate.execute(
                    "alter table recruitment_assessment_feedback add column interviewer_employee_id bigint");
        }

        // Add foreign key for employee assessor if employee_master table exists
        if (tableExists(connection, "employee_master")
                && columnExists(connection, "recruitment_assessment_feedback", "interviewer_employee_id")) {
            jdbcTemplate.execute(
                    "alter table recruitment_assessment_feedback drop constraint "
                            + "if exists fk_assessment_feedback_interviewer_employee");
            jdbcTemplate.execute(
                    "alter table recruitment_assessment_feedback add constraint "
                            + "fk_assessment_feedback_interviewer_employee "
                            + "foreign key (interviewer_employee_id) "
                            + "references employee_master(employee_id)");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) return true;
        }
        try (var rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        var metaData = connection.getMetaData();
        var schema = connection.getSchema();
        try (var columns = metaData.getColumns(null, schema, tableName, columnName)) {
            if (columns.next()) return true;
        }
        try (var columns = metaData.getColumns(null, 
                schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return columns.next();
        }
    }

    private boolean isPostgreSql(Connection connection) throws SQLException {
        String databaseProductName = connection.getMetaData().getDatabaseProductName();
        return databaseProductName != null
                && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
    }
}
