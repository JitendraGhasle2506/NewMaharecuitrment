package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V119__internal_vacancy_multiple_replacement_employees extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists internal_vacancy_replacement_employee (
                        internal_vacancy_replacement_employee_id bigserial primary key,
                        internal_vacancy_opening_id bigint not null,
                        employee_id bigint not null,
                        constraint uk_internal_vacancy_replacement_employee
                            unique (internal_vacancy_opening_id, employee_id),
                        constraint fk_internal_vacancy_replacement_opening
                            foreign key (internal_vacancy_opening_id)
                            references internal_vacancy_opening(internal_vacancy_opening_id)
                            on delete cascade,
                        constraint fk_internal_vacancy_replacement_employee_master
                            foreign key (employee_id)
                            references employee_master(employee_id)
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_internal_vacancy_replacement_opening
                    on internal_vacancy_replacement_employee (internal_vacancy_opening_id)
                    """);
            statement.execute("""
                    create index if not exists idx_internal_vacancy_replacement_employee_master
                    on internal_vacancy_replacement_employee (employee_id)
                    """);
            statement.execute("""
                    do $$
                    begin
                        if exists (
                            select 1
                            from information_schema.columns
                            where table_schema = current_schema()
                              and table_name = 'internal_vacancy_opening'
                              and column_name = 'replacement_employee_id'
                        ) then
                            execute 'insert into internal_vacancy_replacement_employee '
                                || '(internal_vacancy_opening_id, employee_id) '
                                || 'select internal_vacancy_opening_id, replacement_employee_id '
                                || 'from internal_vacancy_opening '
                                || 'where replacement_employee_id is not null '
                                || 'on conflict (internal_vacancy_opening_id, employee_id) do nothing';
                        end if;
                    end $$
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    drop constraint if exists fk_internal_vacancy_replacement_employee
                    """);
            statement.execute("""
                    drop index if exists idx_internal_vacancy_replacement_employee
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    drop column if exists replacement_employee_id,
                    drop column if exists replacement_employee_name
                    """);
        }
    }
}
