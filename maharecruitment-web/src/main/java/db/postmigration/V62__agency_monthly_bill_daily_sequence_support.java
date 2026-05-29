package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V62__agency_monthly_bill_daily_sequence_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table agency_monthly_bill_sequence
                    add column if not exists sequence_date date
                    """);

            statement.execute("""
                    create unique index if not exists uk_agency_bill_sequence_date
                    on agency_monthly_bill_sequence (sequence_date)
                    where sequence_date is not null
                    """);

            statement.execute("""
                    create index if not exists idx_agency_bill_sequence_date
                    on agency_monthly_bill_sequence (sequence_date)
                    """);
        }
    }
}
