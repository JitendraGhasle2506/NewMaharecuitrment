package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__auth_agency_resigned_menu_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        // No-op. Agency resignation menu is maintained by R__auth_reference_data.
        // This class remains only to preserve Flyway version history compatibility.
    }
}
