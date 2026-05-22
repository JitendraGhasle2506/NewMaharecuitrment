package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V58__project_soft_delete_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table project_mst
                    add column if not exists active_flag varchar(1) not null default 'Y'
                    """);

            statement.execute("""
                    update project_mst
                    set active_flag = 'Y'
                    where active_flag is null
                    """);

            statement.execute("""
                    create index if not exists idx_project_mst_active_flag
                    on project_mst (active_flag)
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'ck_project_mst_active_flag'
                              and table_name = 'project_mst'
                        ) then
                            alter table project_mst
                            add constraint ck_project_mst_active_flag
                            check (active_flag in ('Y', 'N'));
                        end if;
                    end $$;
                    """);
        }
    }
}
