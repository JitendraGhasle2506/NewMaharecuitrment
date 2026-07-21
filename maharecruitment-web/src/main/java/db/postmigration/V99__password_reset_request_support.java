package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V99__password_reset_request_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists password_reset_request (
                    reset_request_id bigserial primary key,
                    user_id bigint not null,
                    channel varchar(30) not null,
                    otp_hash varchar(255) not null,
                    otp_expiry_time timestamp not null,
                    otp_verified boolean not null default false,
                    otp_verified_time timestamp,
                    failed_attempts integer not null default 0,
                    max_attempts integer not null default 5,
                    reset_token_hash varchar(64),
                    reset_token_expiry_time timestamp,
                    request_status varchar(30) not null,
                    requested_ip varchar(100),
                    verified_ip varchar(100),
                    reset_ip varchar(100),
                    user_agent varchar(1000),
                    created_on timestamp not null default current_timestamp,
                    updated_on timestamp,
                    completed_on timestamp,
                    version bigint not null default 0,
                    constraint fk_password_reset_user
                        foreign key (user_id) references users(id)
                )
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_password_reset_user
                on password_reset_request (user_id)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_password_reset_status
                on password_reset_request (request_status)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_password_reset_otp_expiry
                on password_reset_request (otp_expiry_time)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_password_reset_token_hash
                on password_reset_request (reset_token_hash)
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
