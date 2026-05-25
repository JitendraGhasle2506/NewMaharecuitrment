package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V61__agency_monthly_bill_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create table if not exists agency_monthly_bill_sequence (
                        agency_monthly_bill_sequence_id bigserial primary key,
                        agency_id bigint not null,
                        bill_year integer not null,
                        bill_month integer not null,
                        last_sequence integer not null default 0,
                        constraint uk_agency_bill_sequence_period unique (agency_id, bill_year, bill_month)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_agency_bill_sequence_period
                    on agency_monthly_bill_sequence (agency_id, bill_year, bill_month)
                    """);

            statement.execute("""
                    create table if not exists agency_monthly_bill (
                        agency_monthly_bill_id bigserial primary key,
                        bill_number varchar(80) not null,
                        agency_id bigint not null,
                        agency_name varchar(200) not null,
                        bill_year integer not null,
                        bill_month integer not null,
                        generated_date date not null,
                        period_from date not null,
                        period_to date not null,
                        days_in_month integer not null,
                        employee_count integer not null,
                        agency_margin_rate numeric(8,4) not null,
                        attendance_amount numeric(14,2) not null,
                        agency_margin_amount numeric(14,2) not null,
                        total_amount numeric(14,2) not null,
                        is_active boolean not null default true,
                        created_by varchar(255) not null,
                        created_date timestamp not null,
                        updated_by varchar(255) not null,
                        updated_date timestamp not null,
                        constraint uk_agency_monthly_bill_number unique (bill_number),
                        constraint uk_agency_monthly_bill_period unique (agency_id, bill_year, bill_month)
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_agency
                    on agency_monthly_bill (agency_id)
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_period
                    on agency_monthly_bill (bill_year, bill_month)
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_generated_date
                    on agency_monthly_bill (generated_date)
                    """);

            statement.execute("""
                    create table if not exists agency_monthly_bill_line_item (
                        agency_monthly_bill_line_item_id bigserial primary key,
                        agency_monthly_bill_id bigint not null,
                        line_no integer not null,
                        employee_id bigint not null,
                        employee_code varchar(50),
                        request_id varchar(50),
                        employee_name varchar(150) not null,
                        designation_id bigint,
                        designation_name varchar(200) not null,
                        level_code varchar(50),
                        monthly_rate numeric(14,2) not null,
                        days_in_month integer not null,
                        payable_days bigint not null,
                        present_days bigint not null,
                        absent_days bigint not null,
                        leave_days bigint not null,
                        comp_off_days bigint not null,
                        tour_days bigint not null,
                        holiday_days bigint not null,
                        week_off_days bigint not null,
                        attendance_amount numeric(14,2) not null,
                        agency_margin_rate numeric(8,4) not null,
                        agency_margin_amount numeric(14,2) not null,
                        line_total numeric(14,2) not null,
                        constraint uk_agency_monthly_bill_line_row unique (agency_monthly_bill_id, line_no),
                        constraint fk_agency_monthly_bill_line_bill
                            foreign key (agency_monthly_bill_id)
                            references agency_monthly_bill(agency_monthly_bill_id)
                            on delete cascade
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_line_bill
                    on agency_monthly_bill_line_item (agency_monthly_bill_id)
                    """);

            statement.execute("""
                    create index if not exists idx_agency_monthly_bill_line_employee
                    on agency_monthly_bill_line_item (employee_id)
                    """);
        }
    }
}
