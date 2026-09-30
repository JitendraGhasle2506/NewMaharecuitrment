package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V136__smtp_configuration extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists smtp_configuration (
                        configuration_id smallint primary key,
                        username varchar(255) not null,
                        password varchar(1000) not null,
                        enabled boolean not null default true,
                        updated_at timestamp not null default current_timestamp,
                        constraint chk_smtp_configuration_singleton check (configuration_id = 1)
                    )
                    """);
        }
    }
}
