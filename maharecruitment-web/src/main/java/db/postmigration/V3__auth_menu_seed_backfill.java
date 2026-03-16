package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__auth_menu_seed_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        new R__auth_reference_data().migrate(context);
    }
}
