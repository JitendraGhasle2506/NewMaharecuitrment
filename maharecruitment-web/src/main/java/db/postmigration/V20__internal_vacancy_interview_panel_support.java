package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Legacy placeholder migration.
 * Version 20 is already present in some databases, so the migration must stay
 * registered locally even though the interview panel feature is not being used.
 */
public class V20__internal_vacancy_interview_panel_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        // Intentionally no-op. Keeps Flyway validation consistent with applied history.
    }
}
