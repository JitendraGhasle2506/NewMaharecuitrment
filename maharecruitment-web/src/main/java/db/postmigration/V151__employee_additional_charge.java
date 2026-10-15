package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V151__employee_additional_charge extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_additional_charge (
                        id bigserial primary key,
                        employee_id bigint references employee_master(employee_id),
                        user_id bigint references users(id),
                        position varchar(100) not null,
                        department_id bigint references department_mst(department_id),
                        cell_id bigint references m_cell_master(cell_id),
                        start_date date not null,
                        end_date date,
                        order_reference varchar(150),
                        responsibilities varchar(1000),
                        assigned_by varchar(255) not null,
                        revoked_at timestamp,
                        revoked_by varchar(255),
                        version bigint not null default 0,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint chk_additional_charge_assignee check ((employee_id is null) <> (user_id is null)),
                        constraint chk_additional_charge_scope check ((department_id is null) <> (cell_id is null)),
                        constraint chk_additional_charge_dates check (end_date is null or end_date >= start_date),
                        constraint chk_additional_charge_position check (length(trim(position)) > 0)
                    )
                    """);
            // Hibernate's schema update may create the table before post-schema migrations run.
            statement.execute("""
                    alter table employee_additional_charge
                        drop constraint if exists chk_additional_charge_assignee,
                        drop constraint if exists chk_additional_charge_scope,
                        drop constraint if exists chk_additional_charge_dates,
                        drop constraint if exists chk_additional_charge_position,
                        add constraint chk_additional_charge_assignee check ((employee_id is null) <> (user_id is null)),
                        add constraint chk_additional_charge_scope check ((department_id is null) <> (cell_id is null)),
                        add constraint chk_additional_charge_dates check (end_date is null or end_date >= start_date),
                        add constraint chk_additional_charge_position check (length(trim(position)) > 0)
                    """);
            statement.execute("""
                    create index if not exists idx_additional_charge_employee
                    on employee_additional_charge(employee_id, start_date, end_date) where revoked_at is null
                    """);
            statement.execute("""
                    create index if not exists idx_additional_charge_user
                    on employee_additional_charge(user_id, start_date, end_date) where revoked_at is null
                    """);
        }
    }
}
