package db.postmigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V100__agency_master_audit_details_text extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("""
                do $$
                declare
                    details_type text;
                begin
                    select udt_name
                    into details_type
                    from information_schema.columns
                    where table_schema = current_schema()
                        and table_name = 'agency_master_audit_log'
                        and column_name = 'details';

                    if details_type is null then
                        return;
                    end if;

                    if details_type = 'oid' then
                        alter table agency_master_audit_log
                            alter column details type text
                            using case
                                when details is null then null
                                when exists (
                                    select 1
                                    from pg_catalog.pg_largeobject_metadata large_object
                                    where large_object.oid = details
                                ) then convert_from(lo_get(details), 'UTF8')
                                else details::text
                            end;
                    elsif details_type <> 'text' then
                        alter table agency_master_audit_log
                            alter column details type text
                            using details::text;
                    end if;
                end $$;
                """);
    }

    private boolean isPostgreSql(Connection connection) {
        try {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.trim().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
