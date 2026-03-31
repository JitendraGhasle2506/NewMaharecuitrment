package db.postmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V43__auth_department_onboarded_employees_direct_menu_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String subMenuTable = resolveTableName(context.getConnection(), "sub_menu_mst");
        if (subMenuTable != null) {
            try (PreparedStatement statement = context.getConnection().prepareStatement(
                    "delete from " + subMenuTable + " where upper(url) = upper(?)")) {
                statement.setString(1, "/department/onboarded");
                statement.executeUpdate();
            }
        }

        new R__auth_reference_data().migrate(context);
    }

    private String resolveTableName(Connection connection, String expectedName) throws SQLException {
        String expectedUpper = expectedName.toUpperCase();
        String sql = """
                select table_name
                from information_schema.tables
                where upper(table_name) = ?
                fetch first 1 row only
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expectedUpper);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }
}
