package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Reconciles older databases once, with the same bounded, transactional guard used by V68. */
public class V134__employee_onboarding_schema_reconciliation extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        EmployeeMasterOnboardingDateSchemaSupport.apply(context.getConnection());
    }
}
