package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V111__login_failure_audit_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "login_logout_audit_history")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                alter table login_logout_audit_history
                    alter column event_type type varchar(20),
                    alter column session_id_hash drop not null,
                    add column if not exists failure_reason varchar(64)
                """);
        jdbcTemplate.execute("""
                alter table login_logout_audit_history
                    drop constraint if exists chk_login_logout_audit_event,
                    drop constraint if exists login_logout_audit_history_event_type_check
                """);
        jdbcTemplate.execute("""
                alter table login_logout_audit_history
                    add constraint chk_login_logout_audit_event
                    check (event_type in ('LOGIN', 'LOGIN_FAILURE', 'LOGOUT'))
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
