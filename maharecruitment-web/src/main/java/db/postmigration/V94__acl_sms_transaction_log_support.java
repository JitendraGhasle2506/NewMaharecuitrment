package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V94__acl_sms_transaction_log_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists sms_transaction_log (
                    sms_transaction_id bigserial primary key,
                    correlation_id varchar(100) not null,
                    user_id bigint,
                    mobile_number_masked varchar(20) not null,
                    sms_type varchar(50) not null,
                    template_id varchar(50) not null,
                    status varchar(30) not null,
                    provider_response varchar(1000),
                    requested_on timestamp not null default current_timestamp,
                    sent_on timestamp,
                    failed_on timestamp,
                    created_on timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                create unique index if not exists uk_sms_transaction_correlation
                on sms_transaction_log(correlation_id)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_sms_transaction_user
                on sms_transaction_log(user_id)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_sms_transaction_status
                on sms_transaction_log(status)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_sms_transaction_requested
                on sms_transaction_log(requested_on)
                """);
        jdbcTemplate.execute("""
                alter table otp_verification_state
                add column if not exists otp_last_sent_at timestamp
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
