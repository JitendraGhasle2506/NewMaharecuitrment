package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V143__internal_conversion_department_optional extends BaseJavaMigration {

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
