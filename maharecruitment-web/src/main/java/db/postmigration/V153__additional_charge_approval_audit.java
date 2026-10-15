package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V153__additional_charge_approval_audit extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            for (String table : new String[]{"leave_application", "tour_application"}) {
                statement.execute("alter table " + table
                        + " add column if not exists manager_decision_by_user_id bigint,"
                        + " add column if not exists hod_decision_by_user_id bigint");
                for (String stage : new String[]{"manager", "hod"}) {
                    statement.execute("create index if not exists idx_" + table + "_" + stage + "_decision_actor on " + table
                            + " (" + stage + "_decision_by_user_id) where " + stage + "_decision_by_user_id is not null");
                }
            }
        }
    }
}
