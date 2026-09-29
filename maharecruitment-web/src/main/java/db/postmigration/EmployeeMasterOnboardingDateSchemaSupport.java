package db.postmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public final class EmployeeMasterOnboardingDateSchemaSupport {
    private static final String TABLE_NAME = "employee_master";
    private static final String OLD_COLUMN = "onboarding_date";
    private static final String NEW_COLUMN = "mahait_onboarding_date";

    private EmployeeMasterOnboardingDateSchemaSupport() { }

    public static boolean apply(Connection connection) throws SQLException {
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")) {
            return false;
        }
        ColumnState columns = inspect(connection);
        // Read catalogs without JDBC getColumns(), which can open the target relation and wait behind DDL.
        if (columns.schema == null || (columns.newColumn && columns.notNull && !columns.oldColumn)) {
            return false;
        }
        boolean ownsTransaction = connection.getAutoCommit();
        boolean safeToResetAutoCommit = ownsTransaction;
        if (ownsTransaction) connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.execute("set local lock_timeout = '5s'");
            String table = quote(columns.schema) + "." + quote(TABLE_NAME);
            boolean oldColumn = columns.oldColumn;
            if (oldColumn && !columns.newColumn) {
                statement.execute("alter table " + table + " rename column " + OLD_COLUMN + " to " + NEW_COLUMN);
                oldColumn = false;
            } else if (!columns.newColumn) {
                statement.execute("alter table " + table + " add column " + NEW_COLUMN + " date");
            }
            if (!columns.notNull) {
                if (oldColumn) {
                    statement.execute("update " + table + " set " + NEW_COLUMN + " = " + OLD_COLUMN
                            + " where " + NEW_COLUMN + " is null and " + OLD_COLUMN + " is not null");
                }
                if (columns.joiningColumn) {
                    statement.execute("update " + table + " set " + NEW_COLUMN
                            + " = joining_date where " + NEW_COLUMN + " is null and joining_date is not null");
                }
                statement.execute("update " + table + " set " + NEW_COLUMN
                        + " = current_date where " + NEW_COLUMN + " is null");
                statement.execute("alter table " + table + " alter column " + NEW_COLUMN + " set not null");
            }
            if (oldColumn) statement.execute("alter table " + table + " drop column " + OLD_COLUMN);
            if (ownsTransaction) connection.commit();
            return true;
        } catch (SQLException | RuntimeException failure) {
            if (ownsTransaction) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) {
                    safeToResetAutoCommit = false;
                    failure.addSuppressed(rollbackFailure);
                    try { connection.abort(Runnable::run); } catch (SQLException abortFailure) { failure.addSuppressed(abortFailure); }
                }
            }
            throw failure;
        } finally {
            if (safeToResetAutoCommit && !connection.isClosed()) connection.setAutoCommit(true);
        }
    }

    private static ColumnState inspect(Connection connection) throws SQLException {
        String schema = null;
        boolean oldColumn = false, newColumn = false, notNull = false, joiningColumn = false;
        try (PreparedStatement statement = connection.prepareStatement("""
                select n.nspname, a.attname, a.attnotnull
                from pg_catalog.pg_class c
                join pg_catalog.pg_namespace n on n.oid = c.relnamespace
                left join pg_catalog.pg_attribute a on a.attrelid = c.oid
                    and a.attnum > 0 and not a.attisdropped
                    and a.attname in ('onboarding_date', 'mahait_onboarding_date', 'joining_date')
                where c.relname = ? and c.relkind in ('r', 'p') and pg_catalog.pg_table_is_visible(c.oid)
                """)) {
            statement.setQueryTimeout(5);
            statement.setString(1, TABLE_NAME);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    schema = rows.getString("nspname");
                    String name = rows.getString("attname");
                    if (OLD_COLUMN.equals(name)) oldColumn = true;
                    if (NEW_COLUMN.equals(name)) { newColumn = true; notNull = rows.getBoolean("attnotnull"); }
                    if ("joining_date".equals(name)) joiningColumn = true;
                }
            }
        }
        return new ColumnState(schema, oldColumn, newColumn, notNull, joiningColumn);
    }

    private static String quote(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
    private record ColumnState(String schema, boolean oldColumn, boolean newColumn, boolean notNull,
                               boolean joiningColumn) { }
}
