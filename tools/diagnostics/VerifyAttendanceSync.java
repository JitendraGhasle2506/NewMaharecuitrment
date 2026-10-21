import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

/** Read-only verification of saved API attendance, with no personal identity numbers in output. */
public final class VerifyAttendanceSync {
    public static void main(String[] args) throws Exception {
        var date = java.time.LocalDate.parse(args[0]);
        Properties settings = new Properties();
        for (String name : new String[] {"application.properties", "application-local.properties"}) {
            try (InputStream input = Files.newInputStream(Path.of("maharecruitment-web/src/main/resources", name))) {
                settings.load(input);
            }
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", settings.getProperty("spring.datasource.username"));
        credentials.setProperty("password", settings.getProperty("spring.datasource.password"));
        credentials.setProperty("ApplicationName", "maharecruitment-attendance-sync-verification");
        credentials.setProperty("connectTimeout", "10");
        credentials.setProperty("socketTimeout", "15");
        credentials.setProperty("options", settings.getProperty("spring.datasource.hikari.data-source-properties.options", ""));
        try (var connection = DriverManager.getConnection(settings.getProperty("spring.datasource.url"), credentials)) {
            connection.setReadOnly(true);
            try (var statement = connection.prepareStatement("""
                    select upper(trim(e.recruitment_type)) as recruitment_type, count(*) as attendance_rows,
                           count(distinct a.employee_id) as employees
                    from daily_attendance_internal_employee a
                    join employee_master e on e.employee_id = a.employee_id
                    where a.attendance_date = ? and a.api_status = 'Y'
                    group by upper(trim(e.recruitment_type)) order by 1
                    """)) {
                statement.setObject(1, date);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) System.out.printf("date=%s type=%s saved_api_rows=%d employees=%d%n",
                            date, rows.getString(1), rows.getLong(2), rows.getLong(3));
                }
            }
            try (var statement = connection.prepareStatement("""
                    select e.employee_code, e.full_name, count(a.id) as rows_today
                    from employee_master e left join daily_attendance_internal_employee a
                      on a.employee_id = e.employee_id and a.attendance_date = ? and a.api_status = 'Y'
                    where lower(e.full_name) in ('dipesh mistry', 'ritik verma', 'pareshkumar diliprao dhangar')
                    group by e.employee_id, e.employee_code, e.full_name order by e.full_name
                    """)) {
                statement.setObject(1, date);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) System.out.printf("code=%s name=%s saved_api_rows_today=%d%n",
                            rows.getString(1), rows.getString(2), rows.getLong(3));
                }
            }
        }
    }
}
