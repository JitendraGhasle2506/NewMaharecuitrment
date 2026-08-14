package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V114__employee_profile_insert_compatibility extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists employee_profile
                    alter column pan_no drop not null
                    """);

            statement.execute("""
                    do $$
                    declare
                        sequence_name text;
                        identity_flag text;
                        next_identifier bigint;
                    begin
                        if to_regclass('employee_profile') is null then
                            return;
                        end if;

                        select pg_get_serial_sequence('employee_profile', 'id')
                        into sequence_name;

                        select is_identity
                        into identity_flag
                        from information_schema.columns
                        where table_schema = current_schema()
                          and table_name = 'employee_profile'
                          and column_name = 'id';

                        if coalesce(identity_flag, 'NO') = 'NO' then
                            if sequence_name is null then
                                create sequence if not exists employee_profile_id_seq;
                                alter sequence employee_profile_id_seq owned by employee_profile.id;
                                sequence_name := 'employee_profile_id_seq';
                            end if;

                            execute format(
                                'alter table employee_profile alter column id set default nextval(%L::regclass)',
                                sequence_name);
                        end if;

                        if sequence_name is not null then
                            select coalesce(max(id), 0) + 1
                            into next_identifier
                            from employee_profile;
                            perform setval(sequence_name::regclass, greatest(next_identifier, 1), false);
                        end if;
                    end $$
                    """);
        }
    }
}
