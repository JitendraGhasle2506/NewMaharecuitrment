package db.postmigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V96__attendance_source_status_flags extends BaseJavaMigration {

    private static final String TABLE_NAME = "daily_attendance_internal_employee";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists mobile_app_status varchar(1) not null default 'N'
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists api_status varchar(1) not null default 'N'
                """);
        jdbcTemplate.execute("""
                update daily_attendance_internal_employee
                set mobile_app_status = case when attendance_source = 'MOBILE_APP' then 'Y' else 'N' end,
                    api_status = case when attendance_source = 'API' then 'Y' else 'N' end
                """);
    }

    private boolean isPostgreSql(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[] { "TABLE" })) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toUpperCase(Locale.ROOT), new String[] { "TABLE" })) {
            return tables.next();
        }
    }
}
