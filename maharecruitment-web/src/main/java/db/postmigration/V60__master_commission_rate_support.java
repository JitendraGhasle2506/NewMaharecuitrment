package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V60__master_commission_rate_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists commission_rate_master (
                        commission_rate_id bigserial primary key,
                        commission_code varchar(20) not null,
                        commission_percentage numeric(5, 2) not null,
                        effective_date date not null,
                        active_flag varchar(1) not null default 'Y',
                        created_user_id bigint,
                        updated_user_id bigint,
                        created_date_time timestamp,
                        updated_date_time timestamp,
                        constraint uk_commission_code_effective_date unique (commission_code, effective_date),
                        constraint ck_commission_rate_code check (commission_code in ('AGENCY', 'MAHAIT')),
                        constraint ck_commission_rate_percentage check (commission_percentage > 0 and commission_percentage <= 100),
                        constraint ck_commission_rate_active_flag check (active_flag in ('Y', 'N'))
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_commission_rate_code_active
                    on commission_rate_master (commission_code, active_flag)
                    """);

            statement.execute("""
                    create index if not exists idx_commission_rate_effective_date
                    on commission_rate_master (effective_date)
                    """);

            statement.execute("""
                    create table if not exists commission_rate_audit_log (
                        audit_id bigserial primary key,
                        commission_rate_id bigint not null,
                        action_type varchar(30) not null,
                        actor_user_id bigint,
                        actor_username varchar(255),
                        action_timestamp timestamp not null default current_timestamp,
                        details text,
                        constraint fk_commission_rate_audit_rate
                            foreign key (commission_rate_id) references commission_rate_master(commission_rate_id)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_commission_rate_audit_rate_id
                    on commission_rate_audit_log (commission_rate_id)
                    """);

            statement.execute("""
                    create index if not exists idx_commission_rate_audit_action_ts
                    on commission_rate_audit_log (action_timestamp)
                    """);
        }
    }
}
