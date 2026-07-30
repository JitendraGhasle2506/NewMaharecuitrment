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

public class V31__recruitment_internal_level_two_feedback_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "recruitment_internal_level_two_schedule")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        if (!tableExists(connection, "recruitment_internal_level_two_feedback")) {
            jdbcTemplate.execute(
                    "create table recruitment_internal_level_two_feedback ("
                            + "recruitment_internal_level_two_feedback_id bigserial primary key, "
                            + "recruitment_internal_level_two_schedule_id bigint not null, "
                            + "reviewer_user_id bigint not null, "
                            + "reviewer_name varchar(150) not null, "
                            + "reviewer_role_label varchar(100) not null, "
                            + "communication_skill_marks integer not null, "
                            + "technical_skill_marks integer not null, "
                            + "leadership_quality_marks integer not null, "
                            + "relevant_experience_marks integer not null, "
                            + "interviewer_grade varchar(10) not null, "
                            + "recommendation_status varchar(30) not null, "
                            + "assessment_remarks varchar(1000), "
                            + "final_remarks varchar(1000), "
                            + "submitted_at timestamp not null, "
                            + "created_date_time timestamp not null default current_timestamp, "
                            + "updated_date_time timestamp not null default current_timestamp"
                            + ")");
        }

        if (!indexExists(connection, "idx_internal_level_two_feedback_schedule")) {
            jdbcTemplate.execute(
                    "create index idx_internal_level_two_feedback_schedule "
                            + "on recruitment_internal_level_two_feedback (recruitment_internal_level_two_schedule_id)");
        }
        if (!indexExists(connection, "idx_internal_level_two_feedback_user")) {
            jdbcTemplate.execute(
                    "create index idx_internal_level_two_feedback_user "
                            + "on recruitment_internal_level_two_feedback (reviewer_user_id)");
        }
        if (!indexExists(connection, "uk_internal_level_two_feedback_schedule_user")) {
            jdbcTemplate.execute(
                    "create unique index uk_internal_level_two_feedback_schedule_user "
                            + "on recruitment_internal_level_two_feedback ("
                            + "recruitment_internal_level_two_schedule_id, reviewer_user_id)");
        }
        if (!constraintExists(connection, "fk_internal_level_two_feedback_schedule")) {
            jdbcTemplate.execute(
                    "alter table recruitment_internal_level_two_feedback add constraint "
                            + "fk_internal_level_two_feedback_schedule "
                            + "foreign key (recruitment_internal_level_two_schedule_id) "
                            + "references recruitment_internal_level_two_schedule(recruitment_internal_level_two_schedule_id)");
        }
        if (tableExists(connection, "users")
                && !constraintExists(connection, "fk_internal_level_two_feedback_user")) {
            jdbcTemplate.execute(
                    "alter table recruitment_internal_level_two_feedback add constraint "
                            + "fk_internal_level_two_feedback_user "
                            + "foreign key (reviewer_user_id) references users(id)");
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
