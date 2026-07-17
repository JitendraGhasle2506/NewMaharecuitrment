package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V97__employee_mobile_photo_support extends BaseJavaMigration {

    private static final String TABLE_NAME = "employee_master";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, TABLE_NAME)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute(
                "alter table employee_master add column if not exists mobile_photo_path varchar(1000)");
        jdbcTemplate.execute("""
                update employee_master
                set mobile_photo_path = photo_path
                where mobile_photo_path is null
                  and ('/' || replace(photo_path, chr(92), '/')) like '%/employee-photo/%'
                """);

        if (tableExists(connection, "agency_candidate_pre_onboarding")) {
            jdbcTemplate.execute("""
                    update agency_candidate_pre_onboarding pre_onboarding
                    set photo_original_name = null,
                        photo_file_path = null,
                        photo_file_type = null,
                        photo_file_size = null
                    where exists (
                        select 1
                        from employee_master employee
                        where employee.pre_onboarding_id = pre_onboarding.pre_onboarding_id
                          and employee.mobile_photo_path = pre_onboarding.photo_file_path
                          and ('/' || replace(pre_onboarding.photo_file_path, chr(92), '/'))
                              like '%/employee-photo/%'
                    )
                    """);
        }

        jdbcTemplate.execute("""
                update employee_master
                set photo_path = null
                where ('/' || replace(photo_path, chr(92), '/')) like '%/employee-photo/%'
                """);
    }

    private boolean tableExists(Connection connection, String tableName) {
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
