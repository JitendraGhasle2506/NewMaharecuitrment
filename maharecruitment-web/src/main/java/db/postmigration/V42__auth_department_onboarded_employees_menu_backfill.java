package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V42__auth_department_onboarded_employees_menu_backfill extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new R__auth_reference_data().migrate(context);
    }
}
