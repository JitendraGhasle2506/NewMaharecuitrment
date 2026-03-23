package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V15__auth_hr_employees_menu_conflict_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new R__auth_reference_data().migrate(context);
    }
}
