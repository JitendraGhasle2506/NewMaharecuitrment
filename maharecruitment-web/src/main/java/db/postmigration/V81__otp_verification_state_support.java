package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V81__otp_verification_state_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists otp_verification_state (
                    otp_state_id bigserial primary key,
                    session_id varchar(128) not null,
                    purpose varchar(120) not null,
                    channel varchar(30) not null,
                    reference_hash varchar(64) not null,
                    reference_masked varchar(120),
                    otp_hash varchar(64),
                    otp_attempt_count integer not null default 0,
                    otp_locked_until timestamp,
                    otp_verified boolean not null default false,
                    otp_expiry_time timestamp,
                    otp_resend_count integer not null default 0,
                    otp_resend_window_start timestamp,
                    otp_last_sent_at timestamp,
                    captcha_id varchar(64),
                    captcha_answer_hash varchar(64),
                    captcha_question varchar(120),
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                create unique index if not exists uk_otp_state_session_purpose_channel
                on otp_verification_state (session_id, purpose, channel)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_otp_state_reference
                on otp_verification_state (purpose, channel, reference_hash)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_otp_state_locked_until
                on otp_verification_state (otp_locked_until)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_otp_state_expiry
                on otp_verification_state (otp_expiry_time)
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
