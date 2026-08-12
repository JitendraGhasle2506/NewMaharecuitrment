package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V108__password_reset_otp_lock_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                alter table password_reset_request
                add column if not exists otp_locked_until timestamp
                """);
        jdbcTemplate.execute("""
                alter table password_reset_request
                alter column otp_hash drop not null
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_password_reset_otp_locked_until
                on password_reset_request (otp_locked_until)
                """);
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
