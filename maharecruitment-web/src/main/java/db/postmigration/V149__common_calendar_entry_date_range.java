package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V149__common_calendar_entry_date_range extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbc.execute("alter table common_calendar_entry add column if not exists end_date date");
        jdbc.execute("""
                create index if not exists idx_common_calendar_entry_range_active
                on common_calendar_entry (entry_date, end_date, is_active)
                """);
        jdbc.execute("""
                do $$
                begin
                    if not exists (
                        select 1 from pg_constraint where conname = 'chk_common_calendar_entry_date_range'
                    ) then
                        alter table common_calendar_entry
                            add constraint chk_common_calendar_entry_date_range
                            check (end_date is null or end_date >= entry_date);
                    end if;
                end $$
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
