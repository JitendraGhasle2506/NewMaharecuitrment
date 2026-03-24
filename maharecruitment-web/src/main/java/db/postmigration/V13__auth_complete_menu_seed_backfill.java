package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__auth_complete_menu_seed_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new R__auth_reference_data().migrate(context);
    }
}
