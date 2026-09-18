package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V125__expand_project_text_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists project_mst
                    alter column project_name type varchar(300)
                    """);
            statement.execute("""
                    alter table if exists project_mst
                    alter column project_desc type varchar(500)
                    """);
        }
    }
}
