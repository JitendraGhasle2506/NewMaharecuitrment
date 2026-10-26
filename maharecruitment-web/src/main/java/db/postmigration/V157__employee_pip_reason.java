package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Nullable for existing plans; new plans require a reason in both form and service validation. */
public class V157__employee_pip_reason extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        try (Statement sql = context.getConnection().createStatement()) {
            sql.execute("alter table employee_pip_plan add column if not exists reason varchar(2000)");
        }
    }
}
