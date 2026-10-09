package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V148__common_calendar_entry_photos extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbc.execute("""
                create table if not exists common_calendar_entry_photo (
                    id bigserial primary key,
                    calendar_entry_id bigint not null references common_calendar_entry(id),
                    original_name varchar(255) not null,
                    stored_name varchar(255) not null,
                    file_path varchar(1000) not null,
                    content_type varchar(100) not null,
                    file_size bigint not null,
                    display_order integer not null default 0,
                    is_active boolean not null default true
                )
                """);
        jdbc.execute("""
                create index if not exists idx_common_calendar_photo_entry_active
                on common_calendar_entry_photo (calendar_entry_id, is_active, display_order)
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
