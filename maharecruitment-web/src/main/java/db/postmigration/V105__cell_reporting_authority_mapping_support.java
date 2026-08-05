package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V105__cell_reporting_authority_mapping_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists cell_reporting_authority_mapping (
                        cell_reporting_authority_mapping_id bigserial primary key,
                        cell_id bigint not null,
                        authority_user_id bigint not null,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_cell_reporting_authority_cell
                            foreign key (cell_id) references m_cell_master(cell_id),
                        constraint fk_cell_reporting_authority_user
                            foreign key (authority_user_id) references users(id),
                        constraint uk_cell_reporting_authority_mapping_cell
                            unique (cell_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_cell_reporting_authority_user
                    on cell_reporting_authority_mapping (authority_user_id)
                    """);

            statement.execute("""
                    create index if not exists idx_employee_reporting_mapping_employee
                    on employee_reporting_mapping (employee_id)
                    """);
        }
    }
}
