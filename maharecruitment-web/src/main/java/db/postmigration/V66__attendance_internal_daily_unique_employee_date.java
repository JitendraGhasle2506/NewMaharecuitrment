package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V66__attendance_internal_daily_unique_employee_date extends BaseJavaMigration {

    private static final String TABLE_NAME = "daily_attendance_internal_employee";
    private static final String INDEX_NAME = "uk_daily_attendance_internal_employee_date";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                delete from daily_attendance_internal_employee daily
                using (
                    select id,
                           row_number() over (
                               partition by employee_id, attendance_date
                               order by
                                   (
                                       case
                                           when upper(coalesce(status, '')) in ('P', 'PRESENT') then 8
                                           when coalesce(status, '') <> ''
                                                and upper(coalesce(status, '')) not in ('A', 'ABSENT') then 4
                                           else 0
                                       end
                                       + case when nullif(trim(coalesce(in_time, '')), '') is not null then 2 else 0 end
                                       + case when nullif(trim(coalesce(out_time, '')), '') is not null then 2 else 0 end
                                       + case when nullif(trim(coalesce(total_hours, '')), '') is not null then 1 else 0 end
                                   ) desc,
                                   id desc
                           ) as row_number
                    from daily_attendance_internal_employee
                    where employee_id is not null
                      and attendance_date is not null
                ) ranked
                where daily.id = ranked.id
                  and ranked.row_number > 1
                """);

        if (!indexExists(connection, TABLE_NAME, INDEX_NAME)) {
            jdbcTemplate.execute("""
                    create unique index uk_daily_attendance_internal_employee_date
                    on daily_attendance_internal_employee (employee_id, attendance_date)
                    """);
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

    private boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getIndexInfo(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                false,
                false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
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
