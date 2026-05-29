package db.postmigration;

import java.sql.Connection;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V68__employee_master_mahait_onboarding_date extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        EmployeeMasterOnboardingDateSchemaSupport.apply(connection);
    }
}
