package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Removes stale Hibernate enum checks created before LOGIN_FAILURE was introduced. */
public class V112__login_failure_legacy_constraint_cleanup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "login_logout_audit_history")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                do $$
                declare
                    stale_constraint record;
                begin
                    for stale_constraint in
                        select conname
                        from pg_constraint
                        where conrelid = 'login_logout_audit_history'::regclass
                          and contype = 'c'
                          and conname <> 'chk_login_logout_audit_event'
                          and pg_get_constraintdef(oid) ilike '%event_type%'
                    loop
                        execute format(
                            'alter table login_logout_audit_history drop constraint %I',
                            stale_constraint.conname);
                    end loop;
                end
                $$
                """);
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet resultSet = connection.getMetaData()
                .getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return resultSet.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
