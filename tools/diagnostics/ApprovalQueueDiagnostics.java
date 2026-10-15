import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Bounded read-only queue diagnostics. Prints routing metadata, never credentials or leave reasons. */
public class ApprovalQueueDiagnostics {
    public static void main(String[] args) throws Exception {
        Properties settings = new Properties();
        for (String file : new String[]{"application.properties", "application-local.properties"}) {
            try (var input = Files.newInputStream(Path.of("maharecruitment-web/src/main/resources", file))) {
                settings.load(input);
            }
        }
        Properties login = new Properties();
        login.setProperty("user", settings.getProperty("spring.datasource.username"));
        login.setProperty("password", settings.getProperty("spring.datasource.password"));
        login.setProperty("ApplicationName", "readonly-approval-queue-diagnostics");
        login.setProperty("connectTimeout", "8");
        login.setProperty("socketTimeout", "15");
        login.setProperty("options", settings.getProperty("spring.datasource.hikari.data-source-properties.options", ""));
        try (var connection = DriverManager.getConnection(settings.getProperty("spring.datasource.url"), login)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            query(connection, 0, """
                select current_schema() as schema, current_date as database_date,
                  (select count(*) from leave_application) as leave_records,
                  (select count(*) from leave_application where status='PENDING') as pending_leave_records,
                  (select count(*) from employee_additional_charge) as charge_records where ?=0
                """);
            query(connection, 0, """
                select a.id, coalesce(holder.full_name, holder_user.name) as holder,
                  coalesce(holder.user_id,a.user_id) as holder_user_id,
                  coalesce(source.full_name,source_user.name) as replacing,
                  a.start_date, a.end_date, a.revoked_at,
                  (select count(*) from additional_charge_team t where t.charge_id=a.id) as team_size
                from employee_additional_charge a
                left join employee_master holder on holder.employee_id=a.employee_id
                left join users holder_user on holder_user.id=a.user_id
                left join employee_master source on source.employee_id=a.replaced_employee_id
                left join users source_user on source_user.id=a.replaced_user_id
                where ?=0 order by a.id desc limit 5
                """);
            try (var lookup = connection.prepareStatement("select u.id, e.employee_id, e.status from users u left join employee_master e on e.user_id=u.id where lower(u.name)=lower(?) limit 3")) {
                lookup.setString(1, args[0]);
                lookup.setQueryTimeout(10);
                try (var users = lookup.executeQuery()) {
                    while (users.next()) {
                        long actor = users.getLong(1);
                        System.out.printf("actor_user=%d employee=%s employee_status=%s%n", actor, users.getString(2), users.getString(3));
                        query(connection, actor, """
                            select c.cell_id, c.authority_level, count(distinct l.leave_id) as pending_cell_leaves
                            from cell_reporting_authority_mapping c
                            left join employee_cell_mapping ec on ec.cell_id=c.cell_id
                            left join leave_application l on l.employee_id=ec.employee_id and l.status='PENDING'
                            where c.authority_user_id=? group by c.cell_id,c.authority_level
                            """);
                        query(connection, actor, """
                            select l.leave_id, e.employee_id, e.employee_code, e.full_name, l.approval_stage,
                              l.manager_approver_user_id, l.hod_approver_user_id,
                              m.hod_user_id as mapped_hod, m.manager_employee_id as mapped_manager,
                              c.cell_id, c.authority_level
                            from cell_reporting_authority_mapping c
                            join employee_cell_mapping ec on ec.cell_id=c.cell_id
                            join employee_master e on e.employee_id=ec.employee_id
                            join leave_application l on l.employee_id=e.employee_id and l.status='PENDING'
                            left join lateral (select r.* from employee_reporting_mapping r
                              where r.employee_id=e.employee_id and coalesce(r.reporting_type,'PRIMARY')='PRIMARY'
                              order by r.mapping_id desc limit 1) m on true
                            where c.authority_user_id=? order by l.leave_id desc limit 20
                            """);
                        query(connection, actor, """
                            select e.employee_id, e.employee_code, e.full_name, m.manager_type, m.reporting_type,
                              (select count(*) from leave_application l where l.employee_id=e.employee_id and l.status='PENDING') as pending_leaves,
                              (select count(*) from leave_application l where l.employee_id=e.employee_id) as total_leaves
                            from employee_reporting_mapping m join employee_master e on e.employee_id=m.employee_id
                            left join employee_master manager on manager.employee_id=m.manager_employee_id
                            where ? in (m.hod_user_id,manager.user_id) order by m.mapping_id desc limit 30
                            """);
                        query(connection, actor, """
                            select a.id, a.position, a.cell_id, a.start_date, a.end_date, a.revoked_at,
                              coalesce(source.user_id,a.replaced_user_id) as source_user,
                              (select count(*) from additional_charge_team t where t.charge_id=a.id) as captured_team
                            from employee_additional_charge a
                            left join employee_master holder on holder.employee_id=a.employee_id
                            left join employee_master source on source.employee_id=a.replaced_employee_id
                            where coalesce(holder.user_id,a.user_id)=? order by a.id desc limit 20
                            """);
                        query(connection, actor, """
                            select l.leave_id, e.employee_code, l.status, l.approval_stage,
                              l.manager_approver_user_id, l.hod_approver_user_id,
                              m.hod_user_id as mapped_hod, manager.user_id as mapped_manager
                            from leave_application l join employee_master e on e.employee_id=l.employee_id
                            left join lateral (select r.* from employee_reporting_mapping r
                              where r.employee_id=e.employee_id and coalesce(r.reporting_type,'PRIMARY')='PRIMARY'
                              order by r.mapping_id desc limit 1) m on true
                            left join employee_master manager on manager.employee_id=m.manager_employee_id
                            where l.status='PENDING' and (
                              ? in (l.manager_approver_user_id,l.hod_approver_user_id,m.hod_user_id,manager.user_id)
                              or exists (select 1 from additional_charge_team t join employee_additional_charge a on a.id=t.charge_id
                                left join employee_master holder on holder.employee_id=a.employee_id
                                where t.employee_id=e.employee_id and coalesce(holder.user_id,a.user_id)=?))
                            order by l.leave_id desc limit 40
                            """);
                    }
                }
            }
            connection.rollback();
        } catch (SQLException failure) {
            System.err.println("Read-only queue diagnostics failed: SQLState=" + failure.getSQLState());
            System.exit(1);
        }
    }
    private static void query(Connection connection, long actor, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            statement.setLong(1, actor);
            if (statement.getParameterMetaData().getParameterCount() == 2) statement.setLong(2, actor);
            try (var rows = statement.executeQuery()) {
                var columns = rows.getMetaData();
                int count = 0;
                while (rows.next()) {
                    count++;
                    for (int i = 1; i <= columns.getColumnCount(); i++) System.out.print(columns.getColumnLabel(i) + "=" + rows.getString(i) + " ");
                    System.out.println();
                }
                if (count == 0) System.out.println("No matching rows.");
            }
        }
    }
}
