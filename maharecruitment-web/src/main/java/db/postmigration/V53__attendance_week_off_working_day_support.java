package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V53__attendance_week_off_working_day_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "week_off_working_day";
    private static final String DATE_INDEX_NAME = "idx_week_off_working_day_date";
    private static final String ACTIVE_INDEX_NAME = "idx_week_off_working_day_active";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!tableExists(connection, TABLE_NAME)) {
            jdbcTemplate.execute("""
                    create table week_off_working_day (
                        id bigserial primary key,
                        working_date date not null,
                        office_order_original_name varchar(255) not null,
                        office_order_stored_name varchar(255) not null,
                        office_order_path varchar(1000) not null,
                        office_order_content_type varchar(150),
                        office_order_file_size bigint,
                        is_active boolean not null default true,
                        created_by varchar(255),
                        created_date timestamp,
                        updated_by varchar(255),
                        updated_date timestamp
                    )
                    """);
        }

        if (!indexExists(connection, TABLE_NAME, DATE_INDEX_NAME)) {
            jdbcTemplate.execute("""
                    create index idx_week_off_working_day_date
                    on week_off_working_day (working_date)
                    """);
        }

        if (!indexExists(connection, TABLE_NAME, ACTIVE_INDEX_NAME)) {
            jdbcTemplate.execute("""
                    create index idx_week_off_working_day_active
                    on week_off_working_day (is_active)
                    """);
        }
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

    private boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getIndexInfo(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                false,
                false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
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
