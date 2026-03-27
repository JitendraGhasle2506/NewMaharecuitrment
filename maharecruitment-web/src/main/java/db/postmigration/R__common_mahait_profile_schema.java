package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__common_mahait_profile_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists mahait_profile_master (
                    mahait_profile_id bigserial primary key,
                    profile_name varchar(150) not null,
                    company_name varchar(200) not null,
                    company_address varchar(1000) not null,
                    cin_number varchar(21) not null,
                    pan_number varchar(10) not null,
                    gst_number varchar(15) not null,
                    bank_name varchar(150) not null,
                    branch_name varchar(150) not null,
                    account_holder_name varchar(150) not null,
                    account_number varchar(30) not null,
                    ifsc_code varchar(11) not null,
                    is_active boolean not null default true,
                    created_by varchar(255) not null,
                    created_date timestamp not null,
                    updated_by varchar(255) not null,
                    updated_date timestamp not null
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_profile_name on mahait_profile_master (profile_name)");
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_company_name on mahait_profile_master (company_name)");
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_cin_number on mahait_profile_master (cin_number)");
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_profile_updated_date on mahait_profile_master (updated_date)");

        jdbcTemplate.execute("""
                create table if not exists mahait_profile_master_audit_log (
                    audit_id bigserial primary key,
                    mahait_profile_id bigint not null,
                    action_type varchar(30) not null,
                    actor_user_id bigint,
                    actor_username varchar(255) not null,
                    action_timestamp timestamp not null default current_timestamp,
                    details text
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_profile_audit_profile_id "
                        + "on mahait_profile_master_audit_log (mahait_profile_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_mahait_profile_audit_action_ts "
                        + "on mahait_profile_master_audit_log (action_timestamp)");
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
