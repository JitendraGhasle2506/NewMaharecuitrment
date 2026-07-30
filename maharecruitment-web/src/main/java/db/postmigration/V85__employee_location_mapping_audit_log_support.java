package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V85__employee_location_mapping_audit_log_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_location_mapping_audit_log (
                        audit_id bigserial primary key,
                        employee_id bigint not null,
                        actor_login_id varchar(255),
                        action_type varchar(50) not null,
                        previous_location_ids text,
                        new_location_ids text,
                        added_location_ids text,
                        removed_location_ids text,
                        summary varchar(255) not null,
                        details text,
                        occurred_at timestamp not null default current_timestamp,
                        constraint fk_employee_location_mapping_audit_employee
                            foreign key (employee_id) references employee_master(employee_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_emp_location_audit_employee
                    on employee_location_mapping_audit_log (employee_id)
                    """);

            statement.execute("""
                    create index if not exists idx_emp_location_audit_actor
                    on employee_location_mapping_audit_log (actor_login_id)
                    """);

            statement.execute("""
                    create index if not exists idx_emp_location_audit_occurred
                    on employee_location_mapping_audit_log (occurred_at)
                    """);
        }
    }
}
