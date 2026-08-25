package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V118__internal_vacancy_replacement_employee_mapping extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists replacement_employee_id bigint
                    """);
            statement.execute("""
                    create index if not exists idx_internal_vacancy_replacement_employee
                    on internal_vacancy_opening (replacement_employee_id)
                    """);
            statement.execute("""
                    do $$
                    begin
                        if to_regclass('internal_vacancy_opening') is not null
                           and to_regclass('employee_master') is not null
                           and not exists (
                               select 1
                               from pg_constraint
                               where conname = 'fk_internal_vacancy_replacement_employee'
                                 and conrelid = to_regclass('internal_vacancy_opening')
                           ) then
                            alter table internal_vacancy_opening
                                add constraint fk_internal_vacancy_replacement_employee
                                foreign key (replacement_employee_id)
                                references employee_master(employee_id);
                        end if;
                    end $$
                    """);
        }
    }
}
