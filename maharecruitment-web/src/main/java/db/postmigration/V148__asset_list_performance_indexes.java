package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V148__asset_list_performance_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create index if not exists idx_asset_master_status_code
                    on asset_master (status, asset_code)
                    """);
            statement.execute("""
                    create index if not exists idx_asset_allocation_status_date
                    on asset_allocation (allocation_status, allocated_date desc, allocation_id desc)
                    """);
        }
    }
}
