package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Adds optional job description document columns to internal vacancy openings.
 */
public class V162__internal_vacancy_job_description_document extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists job_description_file_name varchar(255)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists job_description_file_path varchar(1000)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists job_description_content_type varchar(150)
                    """);
            statement.execute("""
                    alter table if exists internal_vacancy_opening
                    add column if not exists job_description_file_size bigint
                    """);
        }
    }
}
