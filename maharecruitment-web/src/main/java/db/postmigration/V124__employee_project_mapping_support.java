package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V124__employee_project_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_project_mapping (
                        employee_project_mapping_id bigserial primary key,
                        employee_id bigint not null,
                        project_id bigint not null,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_employee_project_mapping_employee
                            foreign key (employee_id) references employee_master(employee_id),
                        constraint fk_employee_project_mapping_project
                            foreign key (project_id) references project_mst(project_id),
                        constraint uk_employee_project_mapping_employee unique (employee_id)
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_employee_project_mapping_employee
                    on employee_project_mapping (employee_id)
                    """);
            statement.execute("""
                    create index if not exists idx_employee_project_mapping_project
                    on employee_project_mapping (project_id)
                    """);
        }
    }
}
