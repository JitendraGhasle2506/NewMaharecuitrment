package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V72__auth_agency_attendance_report_menu_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new R__auth_reference_data().migrate(context);
    }
}
