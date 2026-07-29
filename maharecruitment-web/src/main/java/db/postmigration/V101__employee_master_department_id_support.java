package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V101__employee_master_department_id_support extends BaseJavaMigration {

    private static final String EMPLOYEE_TABLE = "employee_master";
    private static final String DEPARTMENT_TABLE = "department_mst";
    private static final String REGISTRATION_TABLE = "department_registration_master";
    private static final String SUB_DEPARTMENT_TABLE = "sub_department";

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, EMPLOYEE_TABLE)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("alter table employee_master add column if not exists department_id bigint");
        backfillFromSubDepartment(connection, jdbcTemplate);
        backfillFromDepartmentRegistration(connection, jdbcTemplate);
        clearInvalidDepartmentIds(connection, jdbcTemplate);
        addDepartmentForeignKey(connection, jdbcTemplate);
        jdbcTemplate.execute("""
                create index if not exists idx_employee_department_id
                on employee_master (department_id)
                """);
    }

    private void backfillFromSubDepartment(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, SUB_DEPARTMENT_TABLE) || !tableExists(connection, DEPARTMENT_TABLE)) {
            return;
        }
        jdbcTemplate.execute("""
                update employee_master employee
                set department_id = sub_department.department_id
                from sub_department
                where employee.department_id is null
                  and employee.sub_department_id = sub_department.sub_dept_id
                  and exists (
                      select 1
                      from department_mst department
                      where department.department_id = sub_department.department_id
                  )
                """);
    }

    private void backfillFromDepartmentRegistration(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, REGISTRATION_TABLE) || !tableExists(connection, DEPARTMENT_TABLE)) {
            return;
        }
        jdbcTemplate.execute("""
                update employee_master employee
                set department_id = registration.department_id
                from department_registration_master registration
                where employee.department_id is null
                  and employee.department_registration_id = registration.department_registration_id
                  and registration.department_id is not null
                  and exists (
                      select 1
                      from department_mst department
                      where department.department_id = registration.department_id
                  )
                """);
    }

    private void clearInvalidDepartmentIds(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, DEPARTMENT_TABLE)) {
            return;
        }
        jdbcTemplate.execute("""
                update employee_master employee
                set department_id = null
                where employee.department_id is not null
                  and not exists (
                      select 1
                      from department_mst department
                      where department.department_id = employee.department_id
                  )
                """);
    }

    private void addDepartmentForeignKey(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, DEPARTMENT_TABLE)) {
            return;
        }
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_employee_master_department'
                          and conrelid = 'employee_master'::regclass
                    ) then
                        alter table employee_master
                            add constraint fk_employee_master_department
                            foreign key (department_id)
                            references department_mst(department_id);
                    end if;
                end $$;
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
