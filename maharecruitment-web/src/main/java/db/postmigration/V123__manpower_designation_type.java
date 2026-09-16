package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V123__manpower_designation_type extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists manpower_designation_master
                    add column if not exists designation_type varchar(1)
                    """);
            statement.execute("""
                    update manpower_designation_master
                    set designation_type = case
                        when upper(trim(coalesce(designation_type, ''))) = 'M' then 'M'
                        else 'O'
                    end
                    """);
            statement.execute("""
                    alter table manpower_designation_master
                    alter column designation_type set default 'O',
                    alter column designation_type set not null
                    """);
            statement.execute("""
                    alter table manpower_designation_master
                    drop constraint if exists chk_manpower_designation_type
                    """);
            statement.execute("""
                    alter table manpower_designation_master
                    add constraint chk_manpower_designation_type
                    check (designation_type in ('O', 'M'))
                    """);
        }
    }
}
