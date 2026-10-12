package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V147__asset_ownership_and_audit_log extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table asset_master
                    add column if not exists ownership_type varchar(10),
                    add column if not exists rent_start_date date,
                    add column if not exists rent_end_date date
                    """);
            statement.execute("""
                    update asset_master
                    set ownership_type = 'OWNED'
                    where ownership_type is null
                    """);
            statement.execute("""
                    alter table asset_master
                    alter column ownership_type set default 'OWNED',
                    alter column ownership_type set not null
                    """);
            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_constraint
                            where conname = 'chk_asset_ownership_rental_dates'
                              and conrelid = 'asset_master'::regclass
                        ) then
                            alter table asset_master
                            add constraint chk_asset_ownership_rental_dates check (
                                (ownership_type = 'OWNED'
                                    and rent_start_date is null
                                    and rent_end_date is null)
                                or
                                (ownership_type = 'RENTED'
                                    and rent_start_date is not null
                                    and (rent_end_date is null or rent_end_date >= rent_start_date))
                            );
                        end if;
                    end $$
                    """);
            statement.execute("""
                    create table if not exists asset_audit_log (
                        audit_id bigserial primary key,
                        asset_id bigint not null,
                        action_type varchar(30) not null,
                        actor_username varchar(150) not null,
                        ip_address varchar(64) not null,
                        details varchar(2000),
                        occurred_at timestamp not null default current_timestamp,
                        constraint fk_asset_audit_log_asset
                            foreign key (asset_id) references asset_master(asset_id)
                    )
                    """);
            statement.execute("""
                    alter table asset_audit_log
                    alter column occurred_at set default current_timestamp
                    """);
            statement.execute("""
                    create index if not exists idx_asset_audit_log_asset_occurred
                    on asset_audit_log (asset_id, occurred_at desc)
                    """);
            statement.execute("""
                    create index if not exists idx_asset_audit_log_occurred
                    on asset_audit_log (occurred_at desc)
                    """);
        }
    }
}
