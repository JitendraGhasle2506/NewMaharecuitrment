package db.postmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V51__auth_submenu_role_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        createSubMenuRoleTable(context.getConnection());

        String subMenuTable = resolveTableName(context.getConnection(), "sub_menu_mst");
        String menuRoleTable = resolveTableName(context.getConnection(), "menu_role");
        String subMenuRoleTable = resolveTableName(context.getConnection(), "sub_menu_role");
        if (subMenuTable == null || menuRoleTable == null || subMenuRoleTable == null) {
            return;
        }

        try (PreparedStatement statement = context.getConnection().prepareStatement(
                "insert into " + sqlIdentifier(subMenuRoleTable) + " (sub_menu_id, id) "
                        + "select sm.sub_menu_id, mr.id "
                        + "from " + sqlIdentifier(subMenuTable) + " sm "
                        + "join " + sqlIdentifier(menuRoleTable) + " mr on mr.menu_id = sm.menu_id "
                        + "left join " + sqlIdentifier(subMenuRoleTable) + " smr "
                        + "  on smr.sub_menu_id = sm.sub_menu_id and smr.id = mr.id "
                        + "where smr.sub_menu_id is null")) {
            statement.executeUpdate();
        }
    }

    private void createSubMenuRoleTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists sub_menu_role (
                        sub_menu_id bigint not null,
                        id bigint not null,
                        primary key (sub_menu_id, id),
                        constraint fk_sub_menu_role_submenu foreign key (sub_menu_id) references sub_menu_mst(sub_menu_id) on delete cascade,
                        constraint fk_sub_menu_role_role foreign key (id) references roles(id) on delete cascade
                    )
                    """);
        }
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

    private String sqlIdentifier(String tableName) {
        return tableName.chars().anyMatch(Character::isUpperCase) ? "\"" + tableName + "\"" : tableName;
    }
}
