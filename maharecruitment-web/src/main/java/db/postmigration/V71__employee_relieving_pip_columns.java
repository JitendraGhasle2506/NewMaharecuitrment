package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V71__employee_relieving_pip_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS pip_start_date DATE");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS pip_duration VARCHAR(20)");
        }
    }
}
