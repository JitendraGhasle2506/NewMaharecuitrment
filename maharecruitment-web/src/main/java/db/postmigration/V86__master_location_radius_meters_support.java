package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V86__master_location_radius_meters_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists m_location_master
                    add column if not exists radius_meters integer
                    """);

            statement.execute("""
                    update m_location_master
                    set radius_meters = 100
                    where radius_meters is null
                    """);

            statement.execute("""
                    alter table if exists m_location_master
                    alter column radius_meters set default 100
                    """);

            statement.execute("""
                    alter table if exists m_location_master
                    alter column radius_meters set not null
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_constraint
                            where conname = 'ck_m_location_master_radius_meters'
                        ) then
                            alter table m_location_master
                            add constraint ck_m_location_master_radius_meters
                            check (radius_meters between 1 and 10000);
                        end if;
                    end
                    $$;
                    """);
        }
    }
}
