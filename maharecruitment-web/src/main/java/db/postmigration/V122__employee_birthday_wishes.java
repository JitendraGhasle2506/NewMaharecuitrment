package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V122__employee_birthday_wishes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists employee_birthday_wish (
                        wish_id bigserial primary key,
                        recipient_employee_id bigint not null,
                        sender_employee_id bigint not null,
                        celebration_date date not null,
                        wish_message varchar(300) not null,
                        reply_message varchar(300),
                        created_date timestamp not null default current_timestamp,
                        replied_date timestamp,
                        constraint fk_employee_birthday_wish_recipient
                            foreign key (recipient_employee_id) references employee_master(employee_id),
                        constraint fk_employee_birthday_wish_sender
                            foreign key (sender_employee_id) references employee_master(employee_id),
                        constraint uk_employee_birthday_wish_sender_recipient_date
                            unique (sender_employee_id, recipient_employee_id, celebration_date),
                        constraint chk_employee_birthday_wish_not_self
                            check (sender_employee_id <> recipient_employee_id)
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_employee_birthday_wish_recipient_date
                    on employee_birthday_wish (recipient_employee_id, celebration_date)
                    """);
            statement.execute("""
                    create index if not exists idx_employee_birthday_wish_sender_date
                    on employee_birthday_wish (sender_employee_id, celebration_date)
                    """);
        }
    }
}
