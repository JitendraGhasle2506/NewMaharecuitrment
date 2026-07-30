package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V74__employee_relieving_detailed_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS detailed_reason VARCHAR(255)");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS requested_last_working_date DATE");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS notice_period_shortfall VARCHAR(100)");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS active_projects_handover VARCHAR(1000)");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS personal_email VARCHAR(255)");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS alternate_mobile VARCHAR(20)");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS exit_feedback TEXT");
            statement.execute("ALTER TABLE employee_relieving ADD COLUMN IF NOT EXISTS forwarding_address TEXT");
        }
    }
}
