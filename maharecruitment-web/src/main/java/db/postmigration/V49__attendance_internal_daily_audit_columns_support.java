package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V49__attendance_internal_daily_audit_columns_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "daily_attendance_internal_employee";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!columnExists(connection, TABLE_NAME, "created_date")) {
            jdbcTemplate.execute("""
                    alter table daily_attendance_internal_employee
                    add column created_date timestamp
                    """);
        }

        if (!columnExists(connection, TABLE_NAME, "updated_date")) {
            jdbcTemplate.execute("""
                    alter table daily_attendance_internal_employee
                    add column updated_date timestamp
                    """);
        }

        jdbcTemplate.execute("""
                update daily_attendance_internal_employee
                set created_date = coalesce(created_date, current_timestamp),
                    updated_date = coalesce(updated_date, current_timestamp)
                where created_date is null
                   or updated_date is null
                """);

        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column created_date set default current_timestamp
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column updated_date set default current_timestamp
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column created_date set not null
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column updated_date set not null
                """);
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
