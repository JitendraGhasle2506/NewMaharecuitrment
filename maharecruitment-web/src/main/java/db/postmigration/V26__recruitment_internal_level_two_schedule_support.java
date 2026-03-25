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

public class V26__recruitment_internal_level_two_schedule_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_interview_detail")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!tableExists(connection, "recruitment_internal_level_two_schedule")) {
            jdbcTemplate.execute(
                    "create table recruitment_internal_level_two_schedule ("
                            + "recruitment_internal_level_two_schedule_id bigserial primary key, "
                            + "recruitment_interview_detail_id bigint not null, "
                            + "scheduled_by_user_id bigint not null, "
                            + "scheduled_at timestamp not null, "
                            + "interview_date_time timestamp not null, "
                            + "interview_time_slot varchar(100) not null, "
                            + "meeting_link varchar(700), "
                            + "remarks varchar(1000), "
                            + "created_date_time timestamp not null default current_timestamp, "
                            + "updated_date_time timestamp not null default current_timestamp"
                            + ")");
        }

        if (!indexExists(connection, "uk_internal_level_two_schedule_candidate")) {
            jdbcTemplate.execute(
                    "create unique index uk_internal_level_two_schedule_candidate "
                            + "on recruitment_internal_level_two_schedule (recruitment_interview_detail_id)");
        }
        if (!indexExists(connection, "idx_internal_level_two_schedule_candidate")) {
            jdbcTemplate.execute(
                    "create index idx_internal_level_two_schedule_candidate "
                            + "on recruitment_internal_level_two_schedule (recruitment_interview_detail_id)");
        }
        if (!constraintExists(connection, "fk_internal_level_two_schedule_candidate")) {
            jdbcTemplate.execute(
                    "alter table recruitment_internal_level_two_schedule add constraint "
                            + "fk_internal_level_two_schedule_candidate "
                            + "foreign key (recruitment_interview_detail_id) "
                            + "references recruitment_interview_detail(recruitment_interview_detail_id)");
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
