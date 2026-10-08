package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V140__mahait_employee_leave_tour_approval_workflow extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists approval_recruitment_type_rule (
                        recruitment_type_code varchar(30) primary key,
                        active boolean not null default true,
                        created_at timestamp not null default current_timestamp,
                        updated_at timestamp not null default current_timestamp,
                        constraint chk_approval_recruitment_type_code
                            check (recruitment_type_code = upper(trim(recruitment_type_code))
                                and length(trim(recruitment_type_code)) > 0)
                    )
                    """);
            statement.execute("""
                    insert into approval_recruitment_type_rule
                        (recruitment_type_code, active)
                    values ('MAHAIT', true)
                    on conflict (recruitment_type_code) do nothing
                    """);
            statement.execute("""
                    alter table leave_application
                    add column if not exists approval_stage varchar(20),
                    add column if not exists manager_approver_user_id bigint,
                    add column if not exists hod_approver_user_id bigint
                    """);
            statement.execute("""
                    alter table tour_application
                    add column if not exists approval_stage varchar(20),
                    add column if not exists manager_approver_user_id bigint,
                    add column if not exists hod_approver_user_id bigint,
                    add column if not exists manager_remarks varchar(1000)
                    """);
            statement.execute("""
                    create index if not exists idx_leave_approval_stage
                    on leave_application (status, approval_stage, manager_approver_user_id, hod_approver_user_id)
                    """);
            statement.execute("""
                    create index if not exists idx_tour_approval_stage
                    on tour_application (status, approval_stage, manager_approver_user_id, hod_approver_user_id)
                    """);
        }
    }
}
