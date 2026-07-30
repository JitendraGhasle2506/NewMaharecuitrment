package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V28__recruitment_internal_level_two_hr_workflow_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_internal_level_two_schedule")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "panel_assigned_by_user_id",
                "bigint");
        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "panel_assigned_at",
                "timestamp");
        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "hr_time_change_requested",
                "boolean not null default false");
        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "hr_time_change_reason",
                "varchar(1000)");
        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "hr_time_change_requested_at",
                "timestamp");
        addColumnIfMissing(
                jdbcTemplate,
                connection,
                "recruitment_internal_level_two_schedule",
                "hr_time_change_requested_by_user_id",
                "bigint");

        if (!tableExists(connection, "recruitment_internal_level_two_panel_member")) {
            jdbcTemplate.execute(
                    "create table recruitment_internal_level_two_panel_member ("
                            + "recruitment_internal_level_two_panel_member_id bigserial primary key, "
                            + "recruitment_internal_level_two_schedule_id bigint not null, "
                            + "panel_member_name varchar(150) not null, "
                            + "panel_member_designation varchar(150) not null, "
                            + "created_date_time timestamp not null default current_timestamp, "
                            + "updated_date_time timestamp not null default current_timestamp"
                            + ")");
        }

        if (!indexExists(connection, "idx_internal_level_two_panel_member_schedule")) {
            jdbcTemplate.execute(
                    "create index idx_internal_level_two_panel_member_schedule "
                            + "on recruitment_internal_level_two_panel_member (recruitment_internal_level_two_schedule_id)");
        }

        if (!constraintExists(connection, "fk_internal_level_two_panel_member_schedule")) {
            jdbcTemplate.execute(
                    "alter table recruitment_internal_level_two_panel_member add constraint "
                            + "fk_internal_level_two_panel_member_schedule "
                            + "foreign key (recruitment_internal_level_two_schedule_id) "
                            + "references recruitment_internal_level_two_schedule(recruitment_internal_level_two_schedule_id)");
        }
    }

    private void addColumnIfMissing(
            JdbcTemplate jdbcTemplate,
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition) {
        if (!columnExists(connection, tableName, columnName)) {
            jdbcTemplate.execute(
                    "alter table " + tableName + " add column " + columnName + " " + columnDefinition);
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
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getColumns(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean indexExists(Connection connection, String indexName) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select 1 from pg_indexes where lower(indexname) = lower('" + indexName + "')")) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean constraintExists(Connection connection, String constraintName) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select 1 from information_schema.table_constraints "
                                + "where lower(constraint_name) = lower('" + constraintName + "')")) {
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

