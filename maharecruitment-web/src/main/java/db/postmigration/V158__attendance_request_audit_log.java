package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Dedicated audit trail for leave and tour requests and their approvals. */
public class V158__attendance_request_audit_log extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists attendance_request_audit_log (
                        audit_id bigserial primary key,
                        request_type varchar(10) not null,
                        request_id bigint not null,
                        employee_id bigint not null,
                        action_type varchar(20) not null,
                        previous_status varchar(20),
                        new_status varchar(20),
                        previous_stage varchar(20),
                        new_stage varchar(20),
                        request_category varchar(100),
                        start_date date,
                        end_date date,
                        remarks varchar(1000),
                        actor_user_id bigint,
                        actor_name varchar(150),
                        actor_login_id varchar(150),
                        actor_designation varchar(150),
                        actor_roles varchar(500),
                        channel varchar(10) not null,
                        ip_address varchar(64) not null,
                        user_agent varchar(500),
                        details varchar(2000),
                        occurred_at timestamp not null default current_timestamp,
                        constraint chk_attendance_request_audit_type check (request_type in ('LEAVE', 'TOUR'))
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_attendance_request_audit_request
                    on attendance_request_audit_log (request_type, request_id, occurred_at desc)
                    """);
            statement.execute("""
                    create index if not exists idx_attendance_request_audit_employee
                    on attendance_request_audit_log (employee_id, occurred_at desc)
                    """);
            statement.execute("""
                    create index if not exists idx_attendance_request_audit_actor
                    on attendance_request_audit_log (actor_user_id, occurred_at desc)
                    """);
            statement.execute("""
                    create index if not exists idx_attendance_request_audit_occurred
                    on attendance_request_audit_log (occurred_at desc)
                    """);
        }
    }
}
