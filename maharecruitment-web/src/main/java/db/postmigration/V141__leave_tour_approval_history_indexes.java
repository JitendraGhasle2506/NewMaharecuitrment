package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V141__leave_tour_approval_history_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create index if not exists idx_leave_manager_history
                    on leave_application (manager_approver_user_id, status, application_date desc)
                    where manager_approver_user_id is not null
                    """);
            statement.execute("""
                    create index if not exists idx_leave_hod_history
                    on leave_application (hod_approver_user_id, status, application_date desc)
                    where hod_approver_user_id is not null
                    """);
            statement.execute("""
                    create index if not exists idx_leave_employee_history
                    on leave_application (employee_id, status, application_date desc)
                    """);
            statement.execute("""
                    create index if not exists idx_tour_manager_history
                    on tour_application (manager_approver_user_id, status, application_date desc)
                    where manager_approver_user_id is not null
                    """);
            statement.execute("""
                    create index if not exists idx_tour_hod_history
                    on tour_application (hod_approver_user_id, status, application_date desc)
                    where hod_approver_user_id is not null
                    """);
            statement.execute("""
                    create index if not exists idx_tour_employee_history
                    on tour_application (employee_id, status, application_date desc)
                    """);
        }
    }
}
