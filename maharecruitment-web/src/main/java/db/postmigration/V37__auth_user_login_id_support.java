package db.postmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V37__auth_user_login_id_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        // Intentionally no-op. Email remains the login username.
    }
}
