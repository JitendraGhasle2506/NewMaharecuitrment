package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V131__cell_reporting_authority_levels extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table cell_reporting_authority_mapping
                    add column if not exists authority_level integer not null default 1
                    """);
            statement.execute("""
                    alter table cell_reporting_authority_mapping
                    drop constraint if exists uk_cell_reporting_authority_mapping_cell
                    """);
            statement.execute("""
                    create unique index if not exists ux_cell_reporting_authority_level
                    on cell_reporting_authority_mapping (cell_id, authority_level)
                    """);
            statement.execute("""
                    create unique index if not exists ux_cell_reporting_authority_user
                    on cell_reporting_authority_mapping (cell_id, authority_user_id)
                    """);
            statement.execute("""
                    create index if not exists idx_cell_reporting_authority_level
                    on cell_reporting_authority_mapping (cell_id, authority_level)
                    """);
        }
    }
}
