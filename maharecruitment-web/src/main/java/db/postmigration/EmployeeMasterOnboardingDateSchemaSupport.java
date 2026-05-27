package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public final class EmployeeMasterOnboardingDateSchemaSupport {

    private static final String TABLE_NAME = "employee_master";
    private static final String OLD_COLUMN = "onboarding_date";
    private static final String NEW_COLUMN = "mahait_onboarding_date";

    private EmployeeMasterOnboardingDateSchemaSupport() {
    }

    public static boolean apply(Connection connection) throws SQLException {
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return false;
        }

        boolean oldColumnExists = columnExists(connection, TABLE_NAME, OLD_COLUMN);
        boolean newColumnExists = columnExists(connection, TABLE_NAME, NEW_COLUMN);

        try (Statement statement = connection.createStatement()) {
            if (oldColumnExists && !newColumnExists) {
                statement.execute("alter table " + TABLE_NAME + " rename column " + OLD_COLUMN + " to " + NEW_COLUMN);
                oldColumnExists = false;
                newColumnExists = true;
            }

            if (!newColumnExists) {
                statement.execute("alter table " + TABLE_NAME + " add column " + NEW_COLUMN + " date");
                newColumnExists = true;
            }

            if (oldColumnExists) {
                statement.execute(
                        "update " + TABLE_NAME
                                + " set " + NEW_COLUMN + " = " + OLD_COLUMN
                                + " where " + NEW_COLUMN + " is null and " + OLD_COLUMN + " is not null");
            }

            if (newColumnExists && columnExists(connection, TABLE_NAME, "joining_date")) {
                statement.execute(
                        "update " + TABLE_NAME
                                + " set " + NEW_COLUMN + " = joining_date"
                                + " where " + NEW_COLUMN + " is null and joining_date is not null");
            }

            statement.execute(
                    "update " + TABLE_NAME
                            + " set " + NEW_COLUMN + " = current_date"
                            + " where " + NEW_COLUMN + " is null");
            statement.execute("alter table " + TABLE_NAME + " alter column " + NEW_COLUMN + " set not null");

            if (oldColumnExists) {
                statement.execute("alter table " + TABLE_NAME + " drop column " + OLD_COLUMN);
            }
        }
        return true;
    }

    private static boolean tableExists(Connection connection, String tableName) {
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

    private static boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            String schema = connection.getSchema();
            try (ResultSet rs = connection.getMetaData().getColumns(null, schema, tableName, columnName)) {
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

    private static boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
