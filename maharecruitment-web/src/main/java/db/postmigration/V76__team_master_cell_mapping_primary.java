package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V76__team_master_cell_mapping_primary extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table team_master
                    add column if not exists cell_id bigint
                    """);

            statement.execute("""
                    update team_master team
                    set cell_id = project.cell_id
                    from project_mst project
                    where team.project_id = project.project_id
                      and team.cell_id is null
                    """);

            statement.execute("""
                    update team_master
                    set cell_id = (
                        select cell_id
                        from m_cell_master
                        where active_flag = 'Y'
                        order by cell_id
                        fetch first 1 row only
                    )
                    where cell_id is null
                      and exists (select 1 from m_cell_master where active_flag = 'Y')
                    """);

            statement.execute("""
                    alter table team_master
                    alter column project_id drop not null
                    """);

            statement.execute("""
                    alter table team_master
                    drop constraint if exists uk_team_master_project_team
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'fk_team_master_cell'
                              and table_name = 'team_master'
                        ) then
                            alter table team_master
                            add constraint fk_team_master_cell
                            foreign key (cell_id) references m_cell_master(cell_id);
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    create index if not exists idx_team_master_cell_id
                    on team_master (cell_id)
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (select 1 from team_master where cell_id is null) then
                            alter table team_master
                            alter column cell_id set not null;
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'uk_team_master_cell_team'
                              and table_name = 'team_master'
                        )
                        and not exists (
                            select 1
                            from team_master
                            where cell_id is not null
                            group by cell_id, team_name
                            having count(*) > 1
                        ) then
                            alter table team_master
                            add constraint uk_team_master_cell_team unique (cell_id, team_name);
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_indexes
                            where schemaname = current_schema()
                              and indexname = 'ux_team_master_cell_team_lower'
                        )
                        and not exists (
                            select 1
                            from team_master
                            where cell_id is not null
                            group by cell_id, lower(team_name)
                            having count(*) > 1
                        ) then
                            execute 'create unique index ux_team_master_cell_team_lower on team_master (cell_id, lower(team_name))';
                        end if;
                    end $$;
                    """);
        }
    }
}
