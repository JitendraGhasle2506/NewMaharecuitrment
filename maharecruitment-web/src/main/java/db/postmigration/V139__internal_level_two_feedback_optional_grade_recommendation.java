package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V139__internal_level_two_feedback_optional_grade_recommendation extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table if exists recruitment_internal_level_two_feedback
                        alter column interviewer_grade drop not null,
                        alter column recommendation_status drop not null
                    """);
        }
    }
}
