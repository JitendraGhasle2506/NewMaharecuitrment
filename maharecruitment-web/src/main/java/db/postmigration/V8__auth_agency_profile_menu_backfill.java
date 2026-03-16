package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__auth_agency_profile_menu_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        new R__auth_reference_data().migrate(context);
    }
}
