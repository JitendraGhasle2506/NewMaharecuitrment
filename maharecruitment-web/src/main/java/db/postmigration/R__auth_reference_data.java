package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class R__auth_reference_data extends BaseJavaMigration {

    private static final String DEFAULT_PASSWORD_HASH =
            "$2a$10$m41BpG28J2ChSOdJS1jlHO2WTJR.dIkeRW2KekNCa1BkHemVMMr9q";

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "roles") || !tableExists(connection, "users")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        seedRoles(jdbcTemplate);
        seedUsers(jdbcTemplate);
    }

    private void seedRoles(JdbcTemplate jdbcTemplate) {
        List<String> roles = List.of(
                "ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AGENCY", "ROLE_ADMIN",
                "ROLE_USER", "ROLE_STM", "ROLE_HOD", "ROLE_COO",
                "ROLE_MD", "ROLE_PM", "ROLE_AUDITOR", "ROLE_EMPLOYEE");

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
        upsertUser(jdbcTemplate, "md@mahait.org", "Managing Director", "ROLE_MD");
        upsertUser(jdbcTemplate, "auditor@mahait.org", "Auditor User", "ROLE_AUDITOR");
    }

    private void upsertUser(JdbcTemplate jdbcTemplate, String email, String name, String roleName) {
        Long roleId = jdbcTemplate.query(
                "select id from roles where upper(name) = upper(?) fetch first 1 row only",
                rs -> rs.next() ? rs.getLong(1) : null,
                roleName);
        if (roleId == null) {
            return;
        }

        Long userId = jdbcTemplate.query(
                "select id from users where upper(email) = upper(?) fetch first 1 row only",
                rs -> rs.next() ? rs.getLong(1) : null,
                email);

        if (userId == null) {
            jdbcTemplate.update(
                    "insert into users (name, email, password) values (?, ?, ?)",
                    name,
                    email,
                    DEFAULT_PASSWORD_HASH);
            userId = jdbcTemplate.query(
                    "select id from users where upper(email) = upper(?) fetch first 1 row only",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    email);
        } else {
            jdbcTemplate.update(
                    "update users set name = ?, password = coalesce(password, ?) where id = ?",
                    name,
                    DEFAULT_PASSWORD_HASH,
                    userId);
        }

        if (userId == null) {
            return;
        }

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
