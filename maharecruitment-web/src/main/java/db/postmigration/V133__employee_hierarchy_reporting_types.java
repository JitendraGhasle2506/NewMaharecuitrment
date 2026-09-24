package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V133__employee_hierarchy_reporting_types extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_reporting_mapping
                    add column if not exists reporting_type varchar(30) default 'PRIMARY'
                    """);
            statement.execute("update employee_reporting_mapping set reporting_type = 'PRIMARY' where reporting_type is null");
            statement.execute("alter table employee_reporting_mapping alter column reporting_type set default 'PRIMARY'");
            statement.execute("alter table employee_reporting_mapping alter column reporting_type set not null");
            statement.execute("""
                    create index if not exists idx_employee_reporting_type_employee
                    on employee_reporting_mapping (reporting_type, employee_id, mapping_id desc)
                    """);
        }
    }
}
