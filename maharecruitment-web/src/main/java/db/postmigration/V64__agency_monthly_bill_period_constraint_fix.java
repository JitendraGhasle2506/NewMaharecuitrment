package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V64__agency_monthly_bill_period_constraint_fix extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table agency_monthly_bill
                    add column if not exists employee_type varchar(20) not null default 'EXTERNAL'
                    """);

            statement.execute("""
                    alter table agency_monthly_bill_line_item
                    add column if not exists employee_type varchar(20) not null default 'EXTERNAL'
                    """);

            statement.execute("""
                    update agency_monthly_bill
                    set employee_type = 'EXTERNAL'
                    where employee_type is null or trim(employee_type) = ''
                    """);

            statement.execute("""
                    update agency_monthly_bill_line_item
                    set employee_type = 'EXTERNAL'
                    where employee_type is null or trim(employee_type) = ''
                    """);

            statement.execute("""
                    alter table agency_monthly_bill
                    alter column employee_type set default 'EXTERNAL',
                    alter column employee_type set not null
                    """);

            statement.execute("""
                    alter table agency_monthly_bill_line_item
                    alter column employee_type set default 'EXTERNAL',
                    alter column employee_type set not null
                    """);

            statement.execute("""
                    alter table agency_monthly_bill
                    drop constraint if exists uk_agency_monthly_bill_period
                    """);

            statement.execute("""
                    drop index if exists uk_agency_monthly_bill_period
                    """);

            statement.execute("""
                    alter table agency_monthly_bill
                    add constraint uk_agency_monthly_bill_period
                    unique (agency_id, bill_year, bill_month, employee_type)
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_employee_type
                    on agency_monthly_bill (employee_type)
                    """);
        }
    }
}
