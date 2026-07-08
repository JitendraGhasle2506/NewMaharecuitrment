package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V91__employee_fcm_token_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_fcm_tokens (
                        id bigserial primary key,
                        employee_id bigint not null,
                        fcm_token text not null,
                        platform varchar(30) not null,
                        device_id varchar(255) not null,
                        created_at timestamp not null default current_timestamp,
                        updated_at timestamp not null default current_timestamp,
                        constraint fk_employee_fcm_tokens_employee
                            foreign key (employee_id) references employee_master(employee_id),
                        constraint uk_employee_fcm_tokens_employee_device
                            unique (employee_id, device_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_employee_fcm_tokens_employee_id
                    on employee_fcm_tokens (employee_id)
                    """);

            statement.execute("""
                    create index if not exists idx_employee_fcm_tokens_device_id
                    on employee_fcm_tokens (device_id)
                    """);
        }
    }
}
