package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V22__auth_role_name_normalization extends BaseJavaMigration {

    private static final List<String> CANONICAL_ROLE_NAMES = List.of(
            "ROLE_DEPARTMENT",
            "ROLE_HR",
            "ROLE_AGENCY",
            "ROLE_ADMIN",
            "ROLE_USER",
            "ROLE_STM",
            "ROLE_HOD",
            "ROLE_COO",
            "ROLE_PM",
            "ROLE_AUDITOR",
            "ROLE_EMPLOYEE");

    private static final Map<String, String> USER_ROLE_MAPPINGS = Map.ofEntries(
            Map.entry("ADMIN", "ROLE_ADMIN"),
            Map.entry("ROLE_MAHAIT_ADMIN", "ROLE_ADMIN"),
            Map.entry("ROLE_COMMON_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_ATTENDANCE_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_ESERVICEBOOK_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_PENSION_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_HRMS_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_PAYROLL_MANAGER", "ROLE_ADMIN"),
            Map.entry("ROLE_STM1", "ROLE_STM"),
            Map.entry("ROLE_HOD1", "ROLE_HOD"),
            Map.entry("ROLE_HOD2", "ROLE_HOD"),
            Map.entry("ROLE_HOD3", "ROLE_HOD"));

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "roles")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        ensureCanonicalRoles(jdbcTemplate);
        Map<String, Long> roleIds = loadRoleIds(jdbcTemplate);

        if (tableExists(connection, "users_roles")) {
            remapUserRoles(jdbcTemplate, roleIds);
        }

        cleanupObsoleteRoleLinks(jdbcTemplate, connection, roleIds);
        deleteObsoleteRoles(jdbcTemplate, roleIds);

        new R__auth_reference_data().migrate(context);
    }

    private void ensureCanonicalRoles(JdbcTemplate jdbcTemplate) {
        for (String roleName : CANONICAL_ROLE_NAMES) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from roles where upper(name) = upper(?)",
                    Integer.class,
                    roleName);
            if (count != null && count == 0) {
                jdbcTemplate.update("insert into roles (name) values (?)", roleName);
            }
        }
    }

    private Map<String, Long> loadRoleIds(JdbcTemplate jdbcTemplate) {
        Map<String, Long> roleIds = new LinkedHashMap<>();
        jdbcTemplate.query("select id, name from roles", rs -> {
            String roleName = rs.getString("name");
            if (roleName != null) {
                roleIds.put(roleName.trim().toUpperCase(), rs.getLong("id"));
            }
        });
        return roleIds;
    }

    private void remapUserRoles(JdbcTemplate jdbcTemplate, Map<String, Long> roleIds) {
        for (Map.Entry<String, String> entry : USER_ROLE_MAPPINGS.entrySet()) {
            Long sourceRoleId = roleIds.get(entry.getKey());
            Long targetRoleId = roleIds.get(entry.getValue());
            if (sourceRoleId == null || targetRoleId == null || sourceRoleId.equals(targetRoleId)) {
                continue;
            }

            jdbcTemplate.update(
                    "insert into users_roles (user_id, role_id) "
                            + "select ur.user_id, ? from users_roles ur "
                            + "where ur.role_id = ? "
                            + "and not exists ("
                            + "select 1 from users_roles existing "
                            + "where existing.user_id = ur.user_id and existing.role_id = ?)",
                    targetRoleId,
                    sourceRoleId,
                    targetRoleId);
        }
    }

    private void cleanupObsoleteRoleLinks(JdbcTemplate jdbcTemplate, Connection connection, Map<String, Long> roleIds) {
        Set<String> canonicalRoleNameSet = Set.copyOf(CANONICAL_ROLE_NAMES);
        boolean hasUsersRolesTable = tableExists(connection, "users_roles");
        boolean hasMenuRoleTable = tableExists(connection, "menu_role");

        for (Map.Entry<String, Long> entry : roleIds.entrySet()) {
            if (canonicalRoleNameSet.contains(entry.getKey())) {
                continue;
            }

            Long obsoleteRoleId = entry.getValue();
            if (hasUsersRolesTable) {
                jdbcTemplate.update("delete from users_roles where role_id = ?", obsoleteRoleId);
            }
            if (hasMenuRoleTable) {
                jdbcTemplate.update("delete from menu_role where id = ?", obsoleteRoleId);
            }
        }
    }

    private void deleteObsoleteRoles(JdbcTemplate jdbcTemplate, Map<String, Long> roleIds) {
        Set<String> canonicalRoleNameSet = Set.copyOf(CANONICAL_ROLE_NAMES);

        for (Map.Entry<String, Long> entry : roleIds.entrySet()) {
            if (canonicalRoleNameSet.contains(entry.getKey())) {
                continue;
            }
            jdbcTemplate.update("delete from roles where id = ?", entry.getValue());
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
