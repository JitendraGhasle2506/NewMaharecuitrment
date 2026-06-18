package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V79__position_master_cell_mapping extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table position_master
                    add column if not exists cell_id bigint
                    """);

            statement.execute("""
                    update position_master position
                    set cell_id = team.cell_id
                    from team_master team
                    where position.cell_id is null
                      and position.team_id = team.team_id
                    """);

            statement.execute("""
                    update position_master position
                    set cell_id = project.cell_id
                    from project_mst project
                    where position.cell_id is null
                      and position.project_id = project.project_id
                      and project.cell_id is not null
                    """);

            statement.execute("""
                    do $$
                    begin
                        if exists (select 1 from position_master where cell_id is null) then
                            raise exception 'Unable to derive cell_id for every position_master row';
                        end if;
                    end
                    $$
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_constraint
                            where conname = 'fk_position_master_cell'
                        ) then
                            alter table position_master
                            add constraint fk_position_master_cell
                            foreign key (cell_id) references m_cell_master(cell_id);
                        end if;
                    end
                    $$
                    """);

            statement.execute("""
                    alter table position_master
                    alter column cell_id set not null
                    """);

            statement.execute("""
                    create index if not exists idx_position_master_cell_status
                    on position_master (cell_id, status)
                    """);
        }
    }
}
