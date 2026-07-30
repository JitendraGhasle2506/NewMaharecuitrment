package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V77__position_master_level_mapping extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table position_master
                    add column if not exists level_code varchar(10)
                    """);

            statement.execute("""
                    create index if not exists idx_position_master_level
                    on position_master (level_code)
                    """);

            statement.execute("""
                    do $$
                    begin
                        if to_regclass('resource_level_experience_mst') is not null
                           and not exists (
                               select 1
                               from information_schema.table_constraints
                               where constraint_name = 'fk_position_master_level'
                                 and table_name = 'position_master'
                           ) then
                            alter table position_master
                            add constraint fk_position_master_level
                            foreign key (level_code) references resource_level_experience_mst(level_code);
                        end if;
                    end $$;
                    """);
        }
    }
}
