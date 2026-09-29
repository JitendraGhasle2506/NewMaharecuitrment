package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.maharecruitment.gov.in.web.config.PreOnboardingSchemaGuardRunner;

/** Versioned replacement for the legacy startup repair, retained only as a compatibility fallback. */
public class V135__pre_onboarding_schema_reconciliation extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        PreOnboardingSchemaGuardRunner.apply(context.getConnection());
    }
}
