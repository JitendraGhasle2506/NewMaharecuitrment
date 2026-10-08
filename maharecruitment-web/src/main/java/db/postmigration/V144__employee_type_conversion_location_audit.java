package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V144__employee_type_conversion_location_audit extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_type_conversion_audit
                    add column if not exists location_id bigint
                    """);
            statement.execute("""
                    alter table employee_type_conversion_audit
                    add column if not exists location_name varchar(320)
                    """);
        }
    }
}
