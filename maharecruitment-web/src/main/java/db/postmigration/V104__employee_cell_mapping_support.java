package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V104__employee_cell_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_cell_mapping (
                        employee_cell_mapping_id bigserial primary key,
                        employee_id bigint not null,
                        cell_id bigint not null,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_employee_cell_mapping_employee
                            foreign key (employee_id) references employee_master(employee_id),
                        constraint fk_employee_cell_mapping_cell
                            foreign key (cell_id) references m_cell_master(cell_id),
                        constraint uk_employee_cell_mapping_employee
                            unique (employee_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_employee_cell_mapping_employee
                    on employee_cell_mapping (employee_id)
                    """);

            statement.execute("""
                    create index if not exists idx_employee_cell_mapping_cell
                    on employee_cell_mapping (cell_id)
                    """);

            statement.execute("""
                    create table if not exists employee_cell_mapping_audit_log (
                        audit_id bigserial primary key,
                        employee_id bigint not null,
                        actor_login_id varchar(255),
                        action_type varchar(50) not null,
                        previous_cell_id bigint,
                        new_cell_id bigint not null,
                        summary varchar(255) not null,
                        details text,
                        occurred_at timestamp not null default current_timestamp,
                        constraint fk_employee_cell_mapping_audit_employee
                            foreign key (employee_id) references employee_master(employee_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_emp_cell_audit_employee
                    on employee_cell_mapping_audit_log (employee_id)
                    """);

            statement.execute("""
                    create index if not exists idx_emp_cell_audit_actor
                    on employee_cell_mapping_audit_log (actor_login_id)
                    """);

            statement.execute("""
                    create index if not exists idx_emp_cell_audit_occurred
                    on employee_cell_mapping_audit_log (occurred_at)
                    """);
        }
    }
}
