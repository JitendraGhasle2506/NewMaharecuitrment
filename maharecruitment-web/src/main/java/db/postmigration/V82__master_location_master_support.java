package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V82__master_location_master_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists m_location_master (
                        location_id bigserial primary key,
                        location_name varchar(150) not null,
                        latitude numeric(10, 7) not null,
                        longitude numeric(10, 7) not null,
                        active_flag varchar(1) not null default 'Y',
                        created_user_id bigint,
                        updated_user_id bigint,
                        created_date_time timestamp,
                        updated_date_time timestamp,
                        constraint uk_m_location_master_location_name unique (location_name),
                        constraint ck_m_location_master_active_flag check (active_flag in ('Y', 'N')),
                        constraint ck_m_location_master_latitude check (latitude between -90 and 90),
                        constraint ck_m_location_master_longitude check (longitude between -180 and 180)
                    )
                    """);

            statement.execute("""
                    create unique index if not exists ux_m_location_master_location_name_lower
                    on m_location_master (lower(location_name))
                    """);

            statement.execute("""
                    create index if not exists idx_m_location_master_active_flag
                    on m_location_master (active_flag)
                    """);
        }
    }
}
