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

public class V16__project_scope_type_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "project_mst")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!columnExists(connection, "project_mst", "project_scope_type")) {
            jdbcTemplate.execute("alter table project_mst add column project_scope_type varchar(20)");
        }

        jdbcTemplate.execute(
                "update project_mst "
                        + "set project_scope_type = case when application_id is null then 'INTERNAL' else 'EXTERNAL' end "
                        + "where project_scope_type is null or trim(project_scope_type) = ''");
        jdbcTemplate.execute("alter table project_mst alter column project_scope_type set default 'INTERNAL'");
        jdbcTemplate.execute("alter table project_mst alter column project_scope_type set not null");
        jdbcTemplate.execute("alter table project_mst drop constraint if exists project_mst_project_scope_type_check");
        jdbcTemplate.execute(
                "alter table project_mst add constraint project_mst_project_scope_type_check "
                        + "check (project_scope_type in ('INTERNAL', 'EXTERNAL'))");
        jdbcTemplate.execute(
                "create index if not exists idx_project_mst_project_scope_type on project_mst (project_scope_type)");
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
