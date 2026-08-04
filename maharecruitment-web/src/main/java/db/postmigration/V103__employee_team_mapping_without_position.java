package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V103__employee_team_mapping_without_position extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_team_mapping
                    alter column position_id drop not null
                    """);

            statement.execute("""
                    do $$
                    begin
                        if exists (
                            select 1
                            from information_schema.columns
                            where table_name = 'employee_reporting_mapping'
                              and column_name = 'team_id'
                        ) then
                            execute $migration$
                                insert into employee_team_mapping (
                                    employee_id,
                                    team_id,
                                    position_id,
                                    effective_date,
                                    status,
                                    created_date_time,
                                    updated_date_time
                                )
                                select
                                    reporting.employee_id,
                                    reporting.team_id,
                                    null,
                                    current_date,
                                    'ACTIVE',
                                    current_timestamp,
                                    current_timestamp
                                from employee_reporting_mapping reporting
                                where reporting.team_id is not null
                                  and not exists (
                                      select 1
                                      from employee_team_mapping existing
                                      where existing.employee_id = reporting.employee_id
                                        and existing.status = 'ACTIVE'
                                  )
                            $migration$;

                            alter table employee_reporting_mapping
                            drop column team_id cascade;
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    with ranked_active_mappings as (
                        select
                            mapping_id,
                            row_number() over (
                                partition by employee_id
                                order by effective_date desc, mapping_id desc
                            ) as mapping_rank
                        from employee_team_mapping
                        where status = 'ACTIVE'
                          and employee_id is not null
                    )
                    update employee_team_mapping mapping
                    set status = 'INACTIVE',
                        updated_date_time = current_timestamp
                    from ranked_active_mappings ranked
                    where mapping.mapping_id = ranked.mapping_id
                      and ranked.mapping_rank > 1
                    """);

            statement.execute("""
                    create unique index if not exists ux_employee_team_mapping_active_employee
                    on employee_team_mapping (employee_id)
                    where status = 'ACTIVE' and employee_id is not null
                    """);
        }
    }
}
