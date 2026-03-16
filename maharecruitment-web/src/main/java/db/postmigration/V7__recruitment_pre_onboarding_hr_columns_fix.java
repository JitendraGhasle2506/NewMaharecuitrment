package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__recruitment_pre_onboarding_hr_columns_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }
        if (!tableExists(connection, "agency_candidate_pre_onboarding")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "alter table agency_candidate_pre_onboarding "
                            + "add column if not exists hr_onboarding_date date");
            statement.execute(
                    "alter table agency_candidate_pre_onboarding "
                            + "add column if not exists hr_onboarding_location varchar(255)");
            statement.execute(
                    "alter table agency_candidate_pre_onboarding "
                            + "add column if not exists hr_verified boolean not null default false");
            statement.execute(
                    "alter table agency_candidate_pre_onboarding "
                            + "add column if not exists hr_user_id bigint");
            statement.execute(
                    "alter table agency_candidate_pre_onboarding "
                            + "add column if not exists onboarded_at timestamp");

            if (columnExists(connection, "agency_candidate_pre_onboarding", "submitted_at")) {
                statement.execute(
                        "update agency_candidate_pre_onboarding "
                                + "set submitted_at = now() "
                                + "where submitted_at is null");
                statement.execute(
                        "alter table agency_candidate_pre_onboarding "
                                + "alter column submitted_at set not null");
            }
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
        try (ResultSet rs = connection.getMetaData().getTables(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            String schema = connection.getSchema();
            try (ResultSet rs = connection.getMetaData().getColumns(
                    null,
                    schema,
                    tableName,
                    columnName)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = connection.getMetaData().getColumns(
                    null,
                    schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT))) {
                return rs.next();
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
