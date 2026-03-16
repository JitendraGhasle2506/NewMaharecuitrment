package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__auth_menu_seed_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        new R__auth_reference_data().migrate(context);
    }
}
