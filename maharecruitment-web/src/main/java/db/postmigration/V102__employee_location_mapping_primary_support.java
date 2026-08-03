package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V102__employee_location_mapping_primary_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table employee_location_mapping
                    add column if not exists is_primary boolean not null default false
                    """);

            statement.execute("""
                    with ranked_mappings as (
                        select employee_location_mapping_id,
                               row_number() over (
                                   partition by employee_id
                                   order by created_date_time asc nulls last,
                                            employee_location_mapping_id asc
                               ) as mapping_rank
                        from employee_location_mapping
                    )
                    update employee_location_mapping elm
                    set is_primary = (ranked.mapping_rank = 1)
                    from ranked_mappings ranked
                    where ranked.employee_location_mapping_id = elm.employee_location_mapping_id
                    """);

            statement.execute("""
                    create unique index if not exists uk_employee_location_mapping_one_primary
                    on employee_location_mapping (employee_id)
                    where is_primary = true
                    """);
        }
    }
}
