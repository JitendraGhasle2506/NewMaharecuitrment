package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V117__internal_vacancy_hiring_request_details extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists hiring_request_type varchar(30)
                    """);
            statement.execute("""
                    update internal_vacancy_opening
                    set hiring_request_type = 'NEW_CANDIDATE'
                    where hiring_request_type is null
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    alter column hiring_request_type set default 'NEW_CANDIDATE'
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    alter column hiring_request_type set not null
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists replacement_employee_name varchar(200)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists e_office_approval_file_name varchar(255)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists e_office_approval_file_path varchar(1000)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists e_office_approval_content_type varchar(100)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists e_office_approval_file_size bigint
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    drop constraint if exists internal_vacancy_hiring_request_type_check
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add constraint internal_vacancy_hiring_request_type_check
                    check (hiring_request_type in ('NEW_CANDIDATE', 'EMPLOYEE_REPLACEMENT'))
                    """);
        }
    }
}
