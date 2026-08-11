package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V106__project_master_department_mapping extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table project_mst
                        add column if not exists department_id bigint,
                        add column if not exists sub_department_id bigint
                    """);

            statement.execute("""
                    update project_mst project
                    set department_id = case
                            when exists (
                                select 1
                                from department_mst department
                                where department.department_id = application.department_id
                            ) then application.department_id
                            else null
                        end,
                        sub_department_id = case
                            when exists (
                                select 1
                                from sub_department sub_department
                                where sub_department.sub_dept_id = application.sub_department_id
                            ) then application.sub_department_id
                            else null
                        end
                    from department_project_application application
                    where project.application_id = application.department_project_application_id
                    """);

            statement.execute("""
                    do $$
                    begin
                        if exists (
                            select 1
                            from information_schema.columns
                            where table_schema = current_schema()
                              and table_name = 'project_mst'
                              and column_name = 'department_registration_id'
                        ) then
                            execute $migration$
                                update project_mst project
                                set department_id = coalesce(
                                        project.department_id,
                                        case
                                            when exists (
                                                select 1
                                                from department_mst department
                                                where department.department_id = registration.department_id
                                            ) then registration.department_id
                                        end
                                    ),
                                    sub_department_id = coalesce(
                                        project.sub_department_id,
                                        case
                                            when exists (
                                                select 1
                                                from sub_department sub_department
                                                where sub_department.sub_dept_id = registration.sub_department_id
                                            ) then registration.sub_department_id
                                        end
                                    )
                                from department_registration_master registration
                                where project.department_registration_id = registration.department_registration_id
                            $migration$;
                        end if;
                    end $$
                    """);

            statement.execute("""
                    update project_mst project
                    set sub_department_id = null
                    where project.sub_department_id is not null
                      and not exists (
                          select 1
                          from sub_department sub_department
                          where sub_department.sub_dept_id = project.sub_department_id
                      )
                    """);

            statement.execute("""
                    update project_mst project
                    set department_id = sub_department.department_id
                    from sub_department sub_department
                    where project.sub_department_id = sub_department.sub_dept_id
                      and project.department_id is distinct from sub_department.department_id
                    """);

            statement.execute("""
                    update project_mst project
                    set department_id = null
                    where project.department_id is not null
                      and not exists (
                          select 1
                          from department_mst department
                          where department.department_id = project.department_id
                      )
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_constraint
                            where contype = 'f'
                              and conrelid = 'project_mst'::regclass
                              and pg_get_constraintdef(oid) ilike
                                  'FOREIGN KEY (department_id) REFERENCES department_mst%'
                        ) then
                            alter table project_mst
                                add constraint fk_project_mst_department
                                foreign key (department_id)
                                references department_mst(department_id);
                        end if;
                    end $$
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_constraint
                            where contype = 'f'
                              and conrelid = 'project_mst'::regclass
                              and pg_get_constraintdef(oid) ilike
                                  'FOREIGN KEY (sub_department_id) REFERENCES sub_department%'
                        ) then
                            alter table project_mst
                                add constraint fk_project_mst_sub_department
                                foreign key (sub_department_id)
                                references sub_department(sub_dept_id);
                        end if;
                    end $$
                    """);

            statement.execute("""
                    create index if not exists idx_project_mst_department_id
                    on project_mst (department_id)
                    """);

            statement.execute("""
                    create index if not exists idx_project_mst_sub_department_id
                    on project_mst (sub_department_id)
                    """);

            statement.execute("""
                    alter table project_mst
                    drop column if exists department_registration_id
                    """);
        }
    }
}
