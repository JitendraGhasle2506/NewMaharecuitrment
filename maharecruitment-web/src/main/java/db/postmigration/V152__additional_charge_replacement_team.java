package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V152__additional_charge_replacement_team extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_additional_charge
                        add column if not exists replaced_employee_id bigint references employee_master(employee_id),
                        add column if not exists replaced_user_id bigint references users(id)
                    """);
            statement.execute("""
                    alter table employee_additional_charge
                        drop constraint if exists chk_additional_charge_replacement,
                        add constraint chk_additional_charge_replacement
                            check (replaced_employee_id is null or replaced_user_id is null)
                    """);
            statement.execute("""
                    create table if not exists additional_charge_team (
                        charge_id bigint not null references employee_additional_charge(id),
                        employee_id bigint not null,
                        primary key (charge_id, employee_id)
                    )
                    """);
            statement.execute("""
                    alter table additional_charge_team
                        drop constraint if exists fk_additional_charge_team_employee,
                        add constraint fk_additional_charge_team_employee
                            foreign key (employee_id) references employee_master(employee_id)
                    """);
            statement.execute("create unique index if not exists uk_additional_charge_team_member on additional_charge_team(charge_id, employee_id)");
            statement.execute("create index if not exists idx_additional_charge_team_employee on additional_charge_team(employee_id)");
            statement.execute("create index if not exists idx_additional_charge_replacement on employee_additional_charge(cell_id, replaced_employee_id, replaced_user_id, start_date, end_date) where revoked_at is null");
        }
    }
}
