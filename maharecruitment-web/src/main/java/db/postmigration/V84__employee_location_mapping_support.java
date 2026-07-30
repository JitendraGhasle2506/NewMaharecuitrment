package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V84__employee_location_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_location_mapping (
                        employee_location_mapping_id bigserial primary key,
                        employee_id bigint not null,
                        location_id bigint not null,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_employee_location_mapping_employee
                            foreign key (employee_id) references employee_master(employee_id),
                        constraint fk_employee_location_mapping_location
                            foreign key (location_id) references m_location_master(location_id),
                        constraint uk_employee_location_mapping_employee_location
                            unique (employee_id, location_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_employee_location_mapping_employee
                    on employee_location_mapping (employee_id)
                    """);

            statement.execute("""
                    create index if not exists idx_employee_location_mapping_location
                    on employee_location_mapping (location_id)
                    """);
        }
    }
}
