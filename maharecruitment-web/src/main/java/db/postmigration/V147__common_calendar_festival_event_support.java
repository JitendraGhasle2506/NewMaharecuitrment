package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V147__common_calendar_festival_event_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                create table if not exists common_calendar_entry (
                    id bigserial primary key,
                    entry_date date not null,
                    title varchar(150) not null,
                    entry_type varchar(20) not null,
                    description varchar(500),
                    is_active boolean not null default true,
                    constraint chk_common_calendar_entry_type
                        check (entry_type in ('FESTIVAL', 'EVENT'))
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_common_calendar_entry_date_active
                on common_calendar_entry (entry_date, is_active)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_common_calendar_entry_type
                on common_calendar_entry (entry_type)
                """);
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null
                    && productName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
