package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V121__employee_profile_marriage_details extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists employee_profile
                    add column if not exists spouse_name varchar(100),
                    add column if not exists marriage_date date
                    """);
        }
    }
}
