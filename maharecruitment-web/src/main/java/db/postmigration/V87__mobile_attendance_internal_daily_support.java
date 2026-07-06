package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V87__mobile_attendance_internal_daily_support extends BaseJavaMigration {

    private static final String ATTENDANCE_TABLE = "daily_attendance_internal_employee";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, ATTENDANCE_TABLE)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        addColumns(jdbcTemplate);
        backfillEmployeeCode(jdbcTemplate);
        backfillAttendanceSource(jdbcTemplate);
        addConstraintsAndIndexes(jdbcTemplate);
    }

    private void addColumns(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists employee_code varchar(50)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists attendance_source varchar(30)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_in_time timestamp
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_out_time timestamp
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_in_latitude numeric(10,7)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_in_longitude numeric(10,7)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_out_latitude numeric(10,7)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_out_longitude numeric(10,7)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_in_location_address varchar(1000)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_out_location_address varchar(1000)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_in_image_path varchar(1000)
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                add column if not exists check_out_image_path varchar(1000)
                """);
    }

    private void backfillEmployeeCode(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                update daily_attendance_internal_employee attendance
                set employee_code = employee.employee_code
                from employee_master employee
                where attendance.employee_id = employee.employee_id
                  and nullif(trim(coalesce(employee.employee_code, '')), '') is not null
                  and (
                        attendance.employee_code is null
                        or trim(attendance.employee_code) = ''
                        or attendance.employee_code <> employee.employee_code
                  )
                """);
    }

    private void backfillAttendanceSource(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                update daily_attendance_internal_employee
                set attendance_source = 'API'
                where attendance_source is null
                   or trim(attendance_source) = ''
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column attendance_source set default 'API'
                """);
        jdbcTemplate.execute("""
                alter table daily_attendance_internal_employee
                alter column attendance_source set not null
                """);
    }

    private void addConstraintsAndIndexes(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'chk_daily_attendance_internal_source'
                    ) then
                        alter table daily_attendance_internal_employee
                        add constraint chk_daily_attendance_internal_source
                        check (attendance_source in ('API', 'MOBILE_APP', 'WEB'));
                    end if;
                end
                $$;
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_daily_attendance_internal_employee_code
                on daily_attendance_internal_employee (employee_code)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_daily_attendance_internal_source_date
                on daily_attendance_internal_employee (attendance_source, attendance_date)
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
