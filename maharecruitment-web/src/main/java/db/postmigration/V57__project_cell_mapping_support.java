package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V57__project_cell_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table project_mst
                    add column if not exists cell_id bigint
                    """);

            statement.execute("""
                    create index if not exists idx_project_mst_cell_id
                    on project_mst (cell_id)
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'fk_project_mst_cell'
                              and table_name = 'project_mst'
                        ) then
                            alter table project_mst
                            add constraint fk_project_mst_cell
                            foreign key (cell_id) references m_cell_master(cell_id);
                        end if;
                    end $$;
                    """);
        }
    }
}
