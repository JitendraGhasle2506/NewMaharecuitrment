package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__auth_reference_data extends BaseJavaMigration {

        private static final String DEFAULT_PASSWORD_HASH = "$2a$10$m41BpG28J2ChSOdJS1jlHO2WTJR.dIkeRW2KekNCa1BkHemVMMr9q";

        @Override
        public void migrate(Context context) {
                JdbcTemplate jdbcTemplate = jdbcTemplate(context);
                if (!tableExists(context.getConnection(), "roles") || !tableExists(context.getConnection(), "users")) {
                        return;
                }

                seedRoles(jdbcTemplate);
                seedUsers(jdbcTemplate);
                seedMenus(jdbcTemplate, context.getConnection());
        }

        private void seedRoles(JdbcTemplate jdbcTemplate) {
                List<String> roles = List.of(
                                "ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AGENCY", "ROLE_ADMIN",
                                "ROLE_USER", "ROLE_STM", "ROLE_HOD", "ROLE_COO",
                                "ROLE_PM", "ROLE_AUDITOR", "ROLE_EMPLOYEE");

                for (String role : roles) {
                        Integer count = jdbcTemplate.queryForObject(
                                        "select count(*) from roles where upper(name) = upper(?)",
                                        Integer.class,
                                        role);
                        if (count != null && count == 0) {
                                jdbcTemplate.update("insert into roles (name) values (?)", role);
                        }
                }
        }

        private void seedUsers(JdbcTemplate jdbcTemplate) {
                upsertUser(jdbcTemplate, "admin", "System Admin", "ROLE_ADMIN");
                upsertUser(jdbcTemplate, "hr@mahait.org", "HR Manager", "ROLE_HR");
                upsertUser(jdbcTemplate, "auditor@mahait.org", "Auditor User", "ROLE_AUDITOR");
        }

        private void upsertUser(JdbcTemplate jdbcTemplate, String email, String name, String roleName) {
                Long roleId = jdbcTemplate.queryForObject(
                                "select id from roles where upper(name) = upper(?) fetch first 1 row only",
                                Long.class,
                                roleName);
                if (roleId == null) {
                        return;
                }

                Long userId = queryForLong(
                                jdbcTemplate,
                                "select id from users where upper(email) = upper(?) fetch first 1 row only",
                                email);

                if (userId == null) {
                        jdbcTemplate.update(
                                        "insert into users (name, email, password) values (?, ?, ?)",
                                        name,
                                        email,
                                        DEFAULT_PASSWORD_HASH);
                        userId = queryForLong(
                                        jdbcTemplate,
                                        "select id from users where upper(email) = upper(?) fetch first 1 row only",
                                        email);
                } else {
                        jdbcTemplate.update(
                                        "update users set name = ?, password = coalesce(password, ?) where id = ?",
                                        name,
                                        DEFAULT_PASSWORD_HASH,
                                        userId);
                }

                if (userId != null) {
                        Integer count = jdbcTemplate.queryForObject(
                                        "select count(*) from users_roles where user_id = ? and role_id = ?",
                                        Integer.class,
                                        userId,
                                        roleId);
                        if (count != null && count == 0) {
                                jdbcTemplate.update(
                                                "insert into users_roles (user_id, role_id) values (?, ?)",
                                                userId,
                                                roleId);
                        }
                }
        }

        private void seedMenus(JdbcTemplate jdbcTemplate, Connection connection) {
                String menuTable = resolveTableName(connection, "mstmenu", "mst_menu", "MstMenu");
                String subMenuTable = resolveTableName(connection, "sub_menu_mst");
                String menuRoleTable = resolveTableName(connection, "menu_role");

                if (menuTable == null || subMenuTable == null || menuRoleTable == null) {
                        return;
                }

                Map<String, Long> roleIds = loadRoleIds(jdbcTemplate);

                Long adminMenuId = upsertMenu(jdbcTemplate, menuTable, menuRoleTable, "Administration", null,
                                "fa fa-user-shield", 0,
                                roleIds.get("ROLE_ADMIN"));
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Admin Dashboard", "/admin/dashboard",
                                "fa fa-gauge");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Role Management", "/admin/roles",
                                "fa fa-user-tag");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "User Management", "/admin/users",
                                "fa fa-users-cog");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Role Menu Mapping", "/admin/role-menu-mappings",
                                "fa fa-diagram-project");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Menu Management", "/admin/menus", "fa fa-bars");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Submenu Management", "/admin/submenus",
                                "fa fa-sitemap");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "HR Dashboard", "/hr/dashboard",
                                "fa fa-users");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Common Module", "/common",
                                "fa fa-layer-group");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Attendance", "/attendance",
                                "fa fa-calendar-check");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "E-Service Book", "/eservicebook",
                                "fa fa-book");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Pension", "/pension",
                                "fa fa-file-invoice-dollar");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "HRMS", "/hrms",
                                "fa fa-people-group");
                upsertSubMenu(jdbcTemplate, subMenuTable, adminMenuId, "Payroll", "/payroll",
                                "fa fa-money-check-dollar");

                Long masterMenuId = upsertMenu(jdbcTemplate, menuTable, menuRoleTable, "Master Management", null,
                                "fa fa-database", 0,
                                roleIds.get("ROLE_ADMIN"), roleIds.get("ROLE_HR"));
                upsertSubMenu(jdbcTemplate, subMenuTable, masterMenuId, "Designation Master", "/master/designations",
                                "fa fa-id-badge");
                upsertSubMenu(jdbcTemplate, subMenuTable, masterMenuId, "Resource Levels", "/master/resource-levels",
                                "fa fa-layer-group");
                upsertSubMenu(jdbcTemplate, subMenuTable, masterMenuId, "Designation Rates",
                                "/master/designation-rates",
                                "fa fa-coins");
                upsertSubMenu(jdbcTemplate, subMenuTable, masterMenuId, "Project Master", "/master/projects",
                                "fa fa-folder-tree");
                upsertSubMenu(jdbcTemplate, subMenuTable, masterMenuId, "MahaIT Profile",
                                "/common/mahait-profile",
                                "fa fa-building");

                Long departmentMenuId = upsertMenu(jdbcTemplate, menuTable, menuRoleTable, "Department Module", null,
                                "fa fa-building", 0,
                                roleIds.get("ROLE_DEPARTMENT"));
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "Department Dashboard", "/department/home",
                                "fa fa-gauge");
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "Department Profile", "/department/profile",
                                "fa fa-id-card");
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "Manpower Applications",
                                "/department/manpower/list", "fa fa-users-gear");
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "New Manpower Application",
                                "/department/manpower/apply", "fa fa-file-circle-plus");
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "Advance Payments",
                                "/department/payment/list",
                                "fa fa-credit-card");
                upsertSubMenu(jdbcTemplate, subMenuTable, departmentMenuId, "Attendence Reports",
                                "/department/extAttendance",
                                "fa fa-calendar-check");
                Long reviewMenuId = upsertMenu(jdbcTemplate, menuTable, menuRoleTable, "Verification & Approval", null,
                                "fa fa-check-to-slot", 0,
                                roleIds.get("ROLE_HR"), roleIds.get("ROLE_AUDITOR"));
                upsertSubMenu(jdbcTemplate, subMenuTable, reviewMenuId, "Department Payments",
                                "/hr/department/payment/list",
                                "fa fa-receipt");

                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Agency List", "/master/agencies",
                                "fa fa-building",
                                roleIds.get("ROLE_ADMIN"), roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "User Dashboard",
                                "/user/dashboard",
                                "fa fa-user",
                                roleIds.get("ROLE_USER"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "HR Dashboard",
                                "/hr/dashboard",
                                "fa fa-users",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Department Request",
                                "/hr/department-requests",
                                "fa fa-building-user",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Global Agency Rank Mapping",
                                "/hr/department-requests/global-agency-rank-mapping",
                                "fa fa-ranking-star", roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Global Agency Rank Overview",
                                "/hr/department-requests/agency-rank-mapping",
                                "fa fa-table-list", roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Rank Release Overview",
                                "/hr/department-requests/rank-release-overview",
                                "fa fa-hourglass-half", roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Rank Release Rules",
                                "/hr/department-requests/rank-release-rules",
                                "fa fa-list-check", roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Department Audit Request",
                                "/auditor/department-requests",
                                "fa fa-magnifying-glass-chart", roleIds.get("ROLE_AUDITOR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Department Tax Invoice",
                                "/auditor/department-tax-invoices",
                                "fa fa-file-invoice-dollar", roleIds.get("ROLE_AUDITOR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Candidate Shortlisting",
                                "/department/candidate-shortlisting/projects",
                                "fa fa-list-check", roleIds.get("ROLE_DEPARTMENT"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Onboarded Employees",
                                "/department/onboarded",
                                "fa fa-id-badge", roleIds.get("ROLE_DEPARTMENT"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Attendence Reports",
                                "/department/extAttendance",
                                "fa fa-calendar-check", roleIds.get("ROLE_DEPARTMENT"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Pending Onboarding", "/hr/onboarding",
                                "fa fa-user-check",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Internal Vacancy Openings",
                                "/hr/internal-vacancies",
                                "fa fa-clipboard-list",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "All Candidate Details",
                                "/hr/internal-vacancies/candidates",
                                "fa fa-address-card",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Level 2 Interviews",
                                "/hr/internal-vacancies/level-two",
                                "fa fa-calendar-check",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "L2 Eligible Candidates",
                                "/panel/internal-vacancies/level-two",
                                "fa fa-user-check",
                                roleIds.get("ROLE_HOD"),
                                roleIds.get("ROLE_COO"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Interview Shortlisting",
                                "/interview-authority/internal-vacancies",
                                "fa fa-user-check",
                                roleIds.get("ROLE_HOD"),
                                roleIds.get("ROLE_PM"),
                                roleIds.get("ROLE_STM"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Onboarded Candidates", "/hr/employees",
                                "fa fa-users",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Reporting Manager", "/hr/reportingManager",
                                "fa fa-sitemap",
                                roleIds.get("ROLE_HR"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Agency Dashboard", "/agency/dashboard",
                                "fa fa-briefcase",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Agency Profile", "/agency/profile",
                                "fa fa-id-card",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Recruitment Notifications",
                                "/agency/recruitment-notifications", "fa fa-bell",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Shortlisted Candidates",
                                "/agency/shortlisted-candidates",
                                "fa fa-user-clock",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Selected Candidates",
                                "/agency/selected-candidates",
                                "fa fa-user-check",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Internal Assessments",
                                "/agency/internal-assessments",
                                "fa fa-list-check",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Onboarded Employees", "/agency/onboarding",
                                "fa fa-id-card",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Resignation", "/agency/onboarding/resigned",
                                "fa fa-user-minus",
                                roleIds.get("ROLE_AGENCY"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "STM Dashboard", "/stm/dashboard",
                                "fa fa-sitemap",
                                roleIds.get("ROLE_STM"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "PM Dashboard", "/pm/dashboard",
                                "fa fa-diagram-project",
                                roleIds.get("ROLE_PM"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "HOD Dashboard", "/hod1/dashboard",
                                "fa fa-user-tie",
                                roleIds.get("ROLE_HOD"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "COO Dashboard", "/coo/dashboard",
                                "fa fa-chart-line",
                                roleIds.get("ROLE_COO"));
                upsertDirectMenu(jdbcTemplate, menuTable, menuRoleTable, "Employee Dashboard",
                                "/employee/dashboard",
                                "fa fa-id-badge",
                                roleIds.get("ROLE_EMPLOYEE"));
        }

        private Map<String, Long> loadRoleIds(JdbcTemplate jdbcTemplate) {
                Map<String, Long> roleIds = new LinkedHashMap<>();
                jdbcTemplate.query("select id, name from roles", rs -> {
                        roleIds.put(rs.getString("name").toUpperCase(), rs.getLong("id"));
                });
                return roleIds;
        }

        private Long upsertMenu(JdbcTemplate jdbcTemplate, String menuTable, String menuRoleTable, String name,
                        String url,
                        String icon,
                        int isSubMenu, Long... roleIds) {
                String menuTableRef = sqlIdentifier(menuTable);
                String menuRoleTableRef = sqlIdentifier(menuRoleTable);
                Long menuId = null;
                if (url != null && !url.isBlank()) {
                        menuId = queryForLong(
                                        jdbcTemplate,
                                        "select menu_id from " + menuTableRef
                                                        + " where upper(url) = upper(?) fetch first 1 row only",
                                        url);
                } else {
                        menuId = queryForLong(
                                        jdbcTemplate,
                                        "select menu_id from " + menuTableRef
                                                        + " where upper(menu_name_english) = upper(?) fetch first 1 row only",
                                        name);
                }

                if (menuId == null) {
                        jdbcTemplate.update(
                                        "insert into " + menuTableRef
                                                        + " (menu_name_english, menu_name_marathi, is_active, icon, url, is_sub_menu, created_date, updated_date) "
                                                        + "values (?, ?, 'Y', ?, ?, ?, current_timestamp, current_timestamp)",
                                        name,
                                        name,
                                        icon,
                                        url,
                                        isSubMenu);
                        menuId = queryForLong(
                                        jdbcTemplate,
                                        "select menu_id from " + menuTableRef
                                                        + " where upper(menu_name_english) = upper(?) fetch first 1 row only",
                                        name);
                } else {
                        jdbcTemplate.update(
                                        "update " + menuTableRef
                                                        + " set menu_name_english = ?, menu_name_marathi = ?, is_active = 'Y', icon = ?, url = ?, "
                                                        + "is_sub_menu = ?, updated_date = current_timestamp where menu_id = ?",
                                        name,
                                        name,
                                        icon,
                                        url,
                                        isSubMenu,
                                        menuId);
                }

                if (menuId != null) {
                        for (Long roleId : roleIds) {
                                if (roleId == null) {
                                        continue;
                                }
                                Integer count = jdbcTemplate.queryForObject(
                                                "select count(*) from " + menuRoleTableRef
                                                                + " where menu_id = ? and id = ?",
                                                Integer.class,
                                                menuId,
                                                roleId);
                                if (count != null && count == 0) {
                                        jdbcTemplate.update(
                                                        "insert into " + menuRoleTableRef
                                                                        + " (menu_id, id) values (?, ?)",
                                                        menuId,
                                                        roleId);
                                }
                        }
                }
                return menuId;
        }

        private void upsertDirectMenu(JdbcTemplate jdbcTemplate, String menuTable, String menuRoleTable, String name,
                        String url, String icon,
                        Long... roleIds) {
                upsertMenu(jdbcTemplate, menuTable, menuRoleTable, name, url, icon, 1, roleIds);
        }

        private void upsertSubMenu(JdbcTemplate jdbcTemplate, String subMenuTable, Long menuId, String name, String url,
                        String icon) {
                if (menuId == null) {
                        return;
                }

                String subMenuTableRef = sqlIdentifier(subMenuTable);
                Long subMenuId = queryForLong(
                                jdbcTemplate,
                                "select sub_menu_id from " + subMenuTableRef
                                                + " where menu_id = ? and upper(sub_menu_name_english) = upper(?) fetch first 1 row only",
                                menuId,
                                name);
                if (subMenuId == null && url != null) {
                        subMenuId = queryForLong(
                                        jdbcTemplate,
                                        "select sub_menu_id from " + subMenuTableRef
                                                        + " where menu_id = ? and upper(url) = upper(?) fetch first 1 row only",
                                        menuId,
                                        url);
                }

                if (subMenuId == null) {
                        jdbcTemplate.update(
                                        "insert into " + subMenuTableRef
                                                        + " (sub_menu_id, menu_id, sub_menu_name_english, controller_name, url, sub_menu_name_marathi, icon, is_active) "
                                                        + "values (nextval('mstsubmenu_seq'), ?, ?, ?, ?, ?, ?, ?)",
                                        menuId,
                                        name,
                                        name,
                                        url,
                                        name,
                                        icon,
                                        "Y");
                } else {
                        jdbcTemplate.update(
                                        "update " + subMenuTableRef
                                                        + " set menu_id = ?, sub_menu_name_english = ?, controller_name = ?, url = ?, "
                                                        + "sub_menu_name_marathi = ?, icon = ?, is_active = ? where sub_menu_id = ?",
                                        menuId,
                                        name,
                                        name,
                                        url,
                                        name,
                                        icon,
                                        "Y",
                                        subMenuId);
                }
        }

        private JdbcTemplate jdbcTemplate(Context context) {
                return new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        }

        private Long queryForLong(JdbcTemplate jdbcTemplate, String sql, Object... args) {
                return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
        }

        private String resolveTableName(Connection connection, String... candidates) {
                for (String candidate : candidates) {
                        if (tableExists(connection, candidate)) {
                                return candidate;
                        }
                }
                return null;
        }

        private String sqlIdentifier(String tableName) {
                return tableName.chars().anyMatch(Character::isUpperCase) ? "\"" + tableName + "\"" : tableName;
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
