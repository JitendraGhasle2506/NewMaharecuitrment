package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Reconciles databases where the original audit-table constraints remained in
 * place even though internal employees intentionally have no department.
 */
public class V145__employee_type_conversion_department_nullable_reconciliation extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_type_conversion_audit
                    alter column department_id drop not null
                    """);
            statement.execute("""
                    alter table employee_type_conversion_audit
                    alter column department_name drop not null
                    """);
        }
    }
}
