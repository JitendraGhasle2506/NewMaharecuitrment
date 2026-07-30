package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V83__master_location_office_name_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists m_location_master
                    add column if not exists office_name varchar(150)
                    """);

            statement.execute("""
                    create index if not exists idx_m_location_master_office_name_lower
                    on m_location_master (lower(office_name))
                    where office_name is not null
                    """);
        }
    }
}
