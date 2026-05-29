package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V65__agency_monthly_bill_employee_level_billing extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table agency_monthly_bill
                    drop constraint if exists uk_agency_monthly_bill_period
                    """);

            statement.execute("""
                    drop index if exists uk_agency_monthly_bill_period
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_agency_period_active
                    on agency_monthly_bill (agency_id, bill_year, bill_month, is_active)
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_line_employee_bill
                    on agency_monthly_bill_line_item (employee_id, agency_monthly_bill_id)
                    """);
        }
    }
}
