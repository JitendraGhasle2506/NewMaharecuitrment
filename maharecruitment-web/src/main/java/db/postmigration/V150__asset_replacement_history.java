package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V150__asset_replacement_history extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table asset_allocation
                    add column if not exists replaces_allocation_id bigint
                    """);
            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1 from pg_constraint
                            where conname = 'fk_asset_allocation_replaces'
                              and conrelid = 'asset_allocation'::regclass
                        ) then
                            alter table asset_allocation
                            add constraint fk_asset_allocation_replaces
                            foreign key (replaces_allocation_id)
                            references asset_allocation(allocation_id);
                        end if;
                    end $$
                    """);
            statement.execute("""
                    create unique index if not exists uk_asset_allocation_replaces
                    on asset_allocation (replaces_allocation_id)
                    where replaces_allocation_id is not null
                    """);
        }
    }
}
