package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V120__candidate_ctc_and_resignation_details extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    add column if not exists current_ctc numeric(14, 2),
                    add column if not exists has_resigned boolean,
                    add column if not exists last_working_day date
                    """);
            statement.execute("""
                    update recruitment_interview_detail
                    set has_resigned = false
                    where has_resigned is null
                    """);
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    alter column has_resigned set default false,
                    alter column has_resigned set not null
                    """);
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    drop constraint if exists recruitment_interview_current_ctc_check
                    """);
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    add constraint recruitment_interview_current_ctc_check
                    check (current_ctc is null or current_ctc >= 0)
                    """);
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    drop constraint if exists recruitment_interview_last_working_day_check
                    """);
            statement.execute("""
                    alter table if exists recruitment_interview_detail
                    add constraint recruitment_interview_last_working_day_check
                    check (
                        (has_resigned = true and last_working_day is not null)
                        or (has_resigned = false and last_working_day is null)
                    )
                    """);
        }
    }
}
