package db.postmigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V18__recruitment_notification_internal_vacancy_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_notification")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        jdbcTemplate.execute(
                "alter table recruitment_notification alter column department_registration_id drop not null");
        jdbcTemplate.execute(
                "alter table recruitment_notification alter column department_project_application_id drop not null");

        if (tableExists(connection, "internal_vacancy_opening")
                && !columnExists(connection, "recruitment_notification", "internal_vacancy_opening_id")) {
            jdbcTemplate.execute("alter table recruitment_notification add column internal_vacancy_opening_id bigint");
        }

        if (tableExists(connection, "internal_vacancy_opening")
                && columnExists(connection, "recruitment_notification", "internal_vacancy_opening_id")) {
            jdbcTemplate.execute(
                    "create index if not exists idx_recruitment_notification_internal_vacancy "
                            + "on recruitment_notification (internal_vacancy_opening_id)");
            jdbcTemplate.execute(
                    "alter table recruitment_notification drop constraint "
                            + "if exists uk_recruitment_notification_internal_vacancy");
            jdbcTemplate.execute(
                    "alter table recruitment_notification add constraint "
                            + "uk_recruitment_notification_internal_vacancy unique (internal_vacancy_opening_id)");
            jdbcTemplate.execute(
                    "alter table recruitment_notification drop constraint "
                            + "if exists fk_recruitment_notification_internal_vacancy");
            jdbcTemplate.execute(
                    "alter table recruitment_notification add constraint "
                            + "fk_recruitment_notification_internal_vacancy "
                            + "foreign key (internal_vacancy_opening_id) "
                            + "references internal_vacancy_opening(internal_vacancy_opening_id)");
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

    private boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            try (ResultSet columns = metaData.getColumns(null, schema, tableName, columnName)) {
                if (columns.next()) {
                    return true;
                }
            }
            try (ResultSet columns = metaData.getColumns(
                    null,
                    schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT))) {
                return columns.next();
            }
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
