package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__hr_employee_cell_mapping_navigation extends BaseJavaMigration {

    private static final String ROLE_HR = "ROLE_HR";
    private static final String MENU_NAME = "Employee Cell Mapping";
    private static final String MENU_URL = "/hr/employee-cell-mappings";
    private static final String MENU_ICON = "fa-solid fa-table-cells";

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "roles")
                || !tableExists(connection, "mst_menu")
                || !tableExists(connection, "menu_role")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        Long roleId = findRoleId(jdbcTemplate);
        if (roleId == null) {
            return;
        }

        Long menuId = upsertMenu(jdbcTemplate);
        if (menuId != null) {
            ensureMenuRole(jdbcTemplate, menuId, roleId);
        }
    }

    private Long findRoleId(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query(
                "select id from roles where upper(name) = upper(?) fetch first 1 row only",
                rs -> rs.next() ? rs.getLong(1) : null,
                ROLE_HR);
    }

    private Long upsertMenu(JdbcTemplate jdbcTemplate) {
        Long menuId = jdbcTemplate.query(
                "select menu_id from mst_menu where upper(url) = upper(?) fetch first 1 row only",
                rs -> rs.next() ? rs.getLong(1) : null,
                MENU_URL);
        if (menuId == null) {
            menuId = jdbcTemplate.query(
                    "select menu_id from mst_menu where upper(menu_name_english) = upper(?) fetch first 1 row only",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    MENU_NAME);
        }

        if (menuId == null) {
            jdbcTemplate.update(
                    "insert into mst_menu (menu_name_english, menu_name_marathi, is_active, icon, url, is_sub_menu) "
                            + "values (?, ?, 'Y', ?, ?, 1)",
                    MENU_NAME,
                    MENU_NAME,
                    MENU_ICON,
                    MENU_URL);
            return jdbcTemplate.query(
                    "select menu_id from mst_menu where upper(url) = upper(?) fetch first 1 row only",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    MENU_URL);
        }

        jdbcTemplate.update(
                "update mst_menu set menu_name_english = ?, menu_name_marathi = ?, is_active = 'Y', "
                        + "icon = ?, url = ?, is_sub_menu = 1 where menu_id = ?",
                MENU_NAME,
                MENU_NAME,
                MENU_ICON,
                MENU_URL,
                menuId);
        return menuId;
    }

    private void ensureMenuRole(JdbcTemplate jdbcTemplate, Long menuId, Long roleId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from menu_role where menu_id = ? and id = ?",
                Integer.class,
                menuId,
                roleId);
        if (count != null && count == 0) {
            jdbcTemplate.update("insert into menu_role (menu_id, id) values (?, ?)", menuId, roleId);
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

        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select 1 from information_schema.tables where lower(table_name) = lower('"
                                + tableName
                                + "')")) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }
}
