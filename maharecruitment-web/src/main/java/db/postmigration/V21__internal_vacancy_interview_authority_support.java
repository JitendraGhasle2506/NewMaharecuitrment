package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V21__internal_vacancy_interview_authority_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "internal_vacancy_opening")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_interview_role (
                    internal_vacancy_interview_role_id bigserial primary key,
                    internal_vacancy_opening_id bigint not null,
                    role_id bigint not null,
                    created_date_time timestamp not null default current_timestamp,
                    updated_date_time timestamp not null default current_timestamp,
                    constraint uk_internal_vacancy_interview_role
                        unique (internal_vacancy_opening_id, role_id),
                    constraint fk_internal_vacancy_interview_role_opening
                        foreign key (internal_vacancy_opening_id)
                        references internal_vacancy_opening(internal_vacancy_opening_id)
                        on delete cascade,
                    constraint fk_internal_vacancy_interview_role_role
                        foreign key (role_id) references roles(id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_role_opening "
                        + "on internal_vacancy_interview_role (internal_vacancy_opening_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_role_role "
                        + "on internal_vacancy_interview_role (role_id)");

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_interview_authority (
                    internal_vacancy_interview_authority_id bigserial primary key,
                    internal_vacancy_opening_id bigint not null,
                    user_id bigint not null,
                    created_date_time timestamp not null default current_timestamp,
                    updated_date_time timestamp not null default current_timestamp,
                    constraint uk_internal_vacancy_interview_authority
                        unique (internal_vacancy_opening_id, user_id),
                    constraint fk_internal_vacancy_interview_authority_opening
                        foreign key (internal_vacancy_opening_id)
                        references internal_vacancy_opening(internal_vacancy_opening_id)
                        on delete cascade,
                    constraint fk_internal_vacancy_interview_authority_user
                        foreign key (user_id) references users(id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_authority_opening "
                        + "on internal_vacancy_interview_authority (internal_vacancy_opening_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_authority_user "
                        + "on internal_vacancy_interview_authority (user_id)");
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
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
