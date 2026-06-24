package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Legacy resolver for the sensitive identity data encryption migration.
 *
 * <p>Version 80 is already recorded in the UAT post-schema Flyway history.
 * The concrete data encryption is handled by the application-level sensitive
 * data migration/service because it requires the runtime encryption key.
 * Keeping this migration registered prevents Flyway validation from failing
 * when the database has already applied V80.</p>
 */
public class V80__sensitive_identity_data_encryption extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        // Intentionally no-op. Keeps Flyway validation consistent with applied history.
    }
}
