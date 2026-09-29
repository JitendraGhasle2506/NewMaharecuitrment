import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/** Read-only lock inspection. Never prints credentials or query literals. */
public class DatabaseLockDiagnostics {
    public static void main(String[] args) throws Exception {
        Properties settings = new Properties();
        Path resources = Path.of("maharecruitment-web/src/main/resources");
        for (String name : new String[]{"application.properties", "application-local.properties"}) {
            try (InputStream input = Files.newInputStream(resources.resolve(name))) {
                settings.load(input);
            }
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", settings.getProperty("spring.datasource.username"));
        credentials.setProperty("password", settings.getProperty("spring.datasource.password"));
        credentials.setProperty("ApplicationName", "maharecruitment-readonly-lock-diagnostics");
        credentials.setProperty("connectTimeout", "10");
        credentials.setProperty("socketTimeout", "15");
        credentials.setProperty("options", settings.getProperty("spring.datasource.hikari.data-source-properties.options", ""));
        try (Connection connection = DriverManager.getConnection(settings.getProperty("spring.datasource.url"), credentials)) {
            connection.setReadOnly(true);
            if (args.length > 0 && args[0].equals("--check-guard")) {
                Object changed = Class.forName("db.postmigration.EmployeeMasterOnboardingDateSchemaSupport")
                        .getMethod("apply", Connection.class).invoke(null, connection);
                System.out.println("readonly_employee_guard_changed=" + changed);
            }
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(10);
                try (ResultSet setting = statement.executeQuery("show idle_in_transaction_session_timeout")) {
                    setting.next();
                    System.out.println("diagnostic_idle_transaction_timeout=" + setting.getString(1));
                }
                try (ResultSet rows = statement.executeQuery("""
                        select pid, application_name, client_addr::text as client,
                               state, wait_event_type, wait_event,
                               round(extract(epoch from (now() - xact_start))) as transaction_seconds,
                               pg_blocking_pids(pid)::text as blockers,
                               case when query ~* '^\\s*alter\\s+table' then 'ALTER TABLE'
                                    when query ~* '^\\s*select' then 'SELECT'
                                    when query ~* '^\\s*update' then 'UPDATE'
                                    when query ~* '^\\s*insert' then 'INSERT'
                                    when query ~* '^\\s*delete' then 'DELETE'
                                    else 'OTHER' end as query_kind,
                               query ilike '%employee_master%' as mentions_employee_master
                        from pg_stat_activity
                        where datname = current_database() and pid <> pg_backend_pid()
                          and (state like 'idle in transaction%' or cardinality(pg_blocking_pids(pid)) > 0)
                        order by xact_start nulls last
                        """)) {
                    int sessions = 0;
                    while (rows.next()) {
                        sessions++;
                        System.out.printf("pid=%s app=%s client=%s state=%s wait=%s/%s tx_seconds=%s blockers=%s query=%s employee_master=%s%n",
                                rows.getString("pid"), rows.getString("application_name"), rows.getString("client"),
                                rows.getString("state"), rows.getString("wait_event_type"), rows.getString("wait_event"),
                                rows.getString("transaction_seconds"), rows.getString("blockers"),
                                rows.getString("query_kind"), rows.getBoolean("mentions_employee_master"));
                    }
                    if (sessions == 0) System.out.println("No blocked or idle-in-transaction sessions found.");
                }
                try (ResultSet rows = statement.executeQuery("""
                        select a.attname, a.attnotnull
                        from pg_attribute a
                        where a.attrelid = to_regclass('employee_master')
                          and a.attname in ('onboarding_date', 'mahait_onboarding_date', 'joining_date')
                          and not a.attisdropped
                        order by a.attname
                        """)) {
                    while (rows.next()) System.out.printf("column=%s not_null=%s%n", rows.getString(1), rows.getBoolean(2));
                }
            }
        } catch (Exception failure) {
            System.err.println("Read-only database diagnostics failed: " + failure.getClass().getSimpleName());
            System.exit(1);
        }
    }
}
