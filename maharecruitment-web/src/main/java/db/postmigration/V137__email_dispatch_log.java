package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V137__email_dispatch_log extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists email_dispatch_log (
                        email_dispatch_log_id bigserial primary key,
                        sender_email varchar(320),
                        recipient_email varchar(320) not null,
                        reason varchar(500) not null,
                        status varchar(20) not null,
                        failure_reason varchar(1000),
                        requested_at timestamp not null default current_timestamp,
                        sent_at timestamp,
                        failed_at timestamp
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_email_dispatch_recipient
                    on email_dispatch_log (recipient_email)
                    """);
            statement.execute("""
                    create index if not exists idx_email_dispatch_status
                    on email_dispatch_log (status)
                    """);
            statement.execute("""
                    create index if not exists idx_email_dispatch_requested
                    on email_dispatch_log (requested_at)
                    """);
        }
    }
}
