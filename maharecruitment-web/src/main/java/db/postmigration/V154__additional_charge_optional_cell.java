package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V154__additional_charge_optional_cell extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_additional_charge
                        drop constraint if exists chk_additional_charge_scope,
                        add constraint chk_additional_charge_scope check (department_id is null or cell_id is null)
                    """);
        }
    }
}
