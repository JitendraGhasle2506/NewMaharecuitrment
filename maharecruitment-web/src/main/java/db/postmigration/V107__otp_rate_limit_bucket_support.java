package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V107__otp_rate_limit_bucket_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists otp_rate_limit_bucket (
                    bucket_key varchar(64) primary key,
                    window_started_at timestamp with time zone not null,
                    request_count integer not null,
                    last_request_at timestamp with time zone not null,
                    expires_at timestamp with time zone not null,
                    constraint chk_otp_rate_limit_request_count check (request_count > 0)
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_otp_rate_limit_bucket_expires_at
                on otp_rate_limit_bucket (expires_at)
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
