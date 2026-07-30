package db.postmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowStatus;

public class V34__recruitment_internal_level_two_forwarded_status_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "recruitment_internal_level_two_schedule";
    private static final String COLUMN_NAME = "l2_workflow_status";
    private static final String FALLBACK_CONSTRAINT_NAME = "chk_internal_l2_workflow_status";
    private static final String FORWARDED_STATUS = InternalVacancyLevelTwoWorkflowStatus.L2_FORWARDED_TO_AGENCY.name();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)
                || !tableExists(connection, TABLE_NAME)
                || !columnExists(connection, TABLE_NAME, COLUMN_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        ensureWorkflowStatusConstraint(jdbcTemplate, connection);
        jdbcTemplate.execute(
                "update " + TABLE_NAME + " schedule "
                        + "set l2_workflow_status = '" + FORWARDED_STATUS + "' "
                        + "from recruitment_interview_detail candidate "
                        + "where candidate.recruitment_interview_detail_id = schedule.recruitment_interview_detail_id "
                        + "and upper(coalesce(candidate.final_decision_status, '')) in ('SELECTED', 'REJECTED') "
                        + "and upper(coalesce(schedule.l2_workflow_status, '')) in ('L2_SELECTED', 'L2_REJECTED')");
    }

    private void ensureWorkflowStatusConstraint(JdbcTemplate jdbcTemplate, Connection connection) {
        WorkflowStatusConstraint constraint = findWorkflowStatusConstraint(connection);
        String constraintSql = buildWorkflowStatusConstraintSql();
        if (constraint == null) {
            jdbcTemplate.execute(
                    "alter table " + TABLE_NAME + " add constraint " + FALLBACK_CONSTRAINT_NAME + " " + constraintSql);
            return;
        }

        if (constraint.checkClause() != null
                && constraint.checkClause().toUpperCase(Locale.ROOT).contains(FORWARDED_STATUS)) {
            return;
        }

        jdbcTemplate.execute("alter table " + TABLE_NAME + " drop constraint if exists " + constraint.name());
        jdbcTemplate.execute(
                "alter table " + TABLE_NAME + " add constraint " + constraint.name() + " " + constraintSql);
    }

    private WorkflowStatusConstraint findWorkflowStatusConstraint(Connection connection) {
        String sql = "select tc.constraint_name, cc.check_clause "
                + "from information_schema.table_constraints tc "
                + "join information_schema.check_constraints cc "
                + "on tc.constraint_name = cc.constraint_name "
                + "and tc.constraint_schema = cc.constraint_schema "
                + "where lower(tc.table_schema) = lower(current_schema()) "
                + "and lower(tc.table_name) = lower(?) "
                + "and tc.constraint_type = 'CHECK'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TABLE_NAME);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name");
                    String checkClause = rs.getString("check_clause");
                    if (checkClause != null
                            && checkClause.toUpperCase(Locale.ROOT).contains(COLUMN_NAME.toUpperCase(Locale.ROOT))) {
                        return new WorkflowStatusConstraint(constraintName, checkClause);
                    }
                }
            }
        } catch (SQLException ex) {
            return null;
        }
        return null;
    }

    private String buildWorkflowStatusConstraintSql() {
        return "check (upper(coalesce(" + COLUMN_NAME + ", '')) in ("
                + Arrays.stream(InternalVacancyLevelTwoWorkflowStatus.values())
                        .map(Enum::name)
                        .map(this::quoteSqlLiteral)
                        .collect(Collectors.joining(", "))
                + "))";
    }

    private String quoteSqlLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
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

    private boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }

    private record WorkflowStatusConstraint(String name, String checkClause) {
    }
}
