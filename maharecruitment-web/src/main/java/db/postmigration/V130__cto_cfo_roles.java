package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V130__cto_cfo_roles extends BaseJavaMigration {

    private static final String[] ROLE_NAMES = {"ROLE_CTO", "ROLE_CFO"};

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "roles")) {
            return;
        }

        try (var statement = connection.prepareStatement(
                "insert into roles (name) "
                        + "select ? where not exists (select 1 from roles where upper(name) = upper(?))")) {
            for (String roleName : ROLE_NAMES) {
                statement.setString(1, roleName);
                statement.setString(2, roleName);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (tables.next()) {
                return true;
            }
        }
        try (var statement = connection.prepareStatement(
                "select 1 from information_schema.tables where lower(table_name) = lower(?)")) {
            statement.setString(1, tableName);
            try (ResultSet tables = statement.executeQuery()) {
                return tables.next();
            }
        }
    }
}
