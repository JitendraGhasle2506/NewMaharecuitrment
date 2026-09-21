package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V126__deactivate_internal_projects extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    update project_mst
                    set active_flag = 'N'
                    where project_scope_type = 'INTERNAL'
                      and project_id in (3, 4, 5, 34, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54)
                    """);
        }
    }
}
