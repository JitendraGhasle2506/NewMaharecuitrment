package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V132__multiple_level_two_cell_authorities extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table cell_reporting_authority_mapping
                    drop constraint if exists ux_cell_reporting_authority_level
                    """);
            statement.execute("""
                    drop index if exists ux_cell_reporting_authority_level
                    """);
            statement.execute("""
                    create unique index if not exists ux_cell_reporting_primary_authority
                    on cell_reporting_authority_mapping (cell_id)
                    where authority_level = 1
                    """);
        }
    }
}
