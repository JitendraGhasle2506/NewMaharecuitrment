package db.postmigration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V156__employee_pip_reviews extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        try (Statement sql = context.getConnection().createStatement()) {
            sql.execute("""
                    create table if not exists employee_pip_plan (
                        id bigserial primary key,
                        employee_id bigint not null,
                        created_by_user_id bigint not null,
                        start_date date not null, duration_months integer not null check (duration_months in (1, 2)),
                        end_date date not null, objectives varchar(2000) not null,
                        status varchar(30) not null default 'UNDER_REVIEW',
                        decision_by_user_id bigint, decision_date date,
                        decision_feedback varchar(2000), exit_date date, version bigint not null default 0,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp
                    )
                    """);
            sql.execute("""
                    create table if not exists employee_pip_review (
                        id bigserial primary key, plan_id bigint not null,
                        reviewer_user_id bigint not null, reviewer_name varchar(255) not null,
                        review_date date not null, rating integer not null check (rating between 1 and 5),
                        feedback varchar(2000) not null,
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp
                    )
                    """);
            sql.execute("create unique index if not exists uq_employee_pip_active on employee_pip_plan(employee_id) where status = 'UNDER_REVIEW'");
            // Reconcile constraints even when Hibernate created the tables before post-schema migrations.
            sql.execute("""
                    alter table employee_pip_plan
                        drop constraint if exists chk_pip_period,
                        drop constraint if exists chk_pip_status,
                        drop constraint if exists fk_pip_employee,
                        drop constraint if exists fk_pip_creator,
                        drop constraint if exists fk_pip_decision_user,
                        add constraint chk_pip_period check (duration_months in (1, 2) and end_date >= start_date),
                        add constraint chk_pip_status check (status in ('UNDER_REVIEW', 'COMPLETED', 'EXIT_RECORDED')),
                        add constraint fk_pip_employee foreign key (employee_id) references employee_master(employee_id),
                        add constraint fk_pip_creator foreign key (created_by_user_id) references users(id),
                        add constraint fk_pip_decision_user foreign key (decision_by_user_id) references users(id)
                    """);
            sql.execute("""
                    alter table employee_pip_review
                        drop constraint if exists chk_pip_rating,
                        drop constraint if exists fk_pip_review_plan,
                        drop constraint if exists fk_pip_review_user,
                        add constraint chk_pip_rating check (rating between 1 and 5),
                        add constraint fk_pip_review_plan foreign key (plan_id) references employee_pip_plan(id),
                        add constraint fk_pip_review_user foreign key (reviewer_user_id) references users(id)
                    """);
            sql.execute("create index if not exists idx_employee_pip_history on employee_pip_plan(employee_id, created_date_time desc, id desc)");
            sql.execute("create index if not exists idx_employee_pip_reviews on employee_pip_review(plan_id, review_date desc, id desc)");
        }
    }
}
