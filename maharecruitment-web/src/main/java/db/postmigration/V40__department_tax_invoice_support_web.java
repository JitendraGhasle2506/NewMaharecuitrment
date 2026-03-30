package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V40__department_tax_invoice_support_web extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        jdbcTemplate.execute("""
                create table if not exists department_tax_invoice_sequence (
                    department_tax_invoice_sequence_id bigserial primary key,
                    financial_year_code varchar(9) not null,
                    last_sequence integer not null default 0,
                    constraint uk_dep_tax_invoice_sequence_fy unique (financial_year_code)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_sequence_fy "
                        + "on department_tax_invoice_sequence (financial_year_code)");

        jdbcTemplate.execute("""
                create table if not exists department_tax_invoice (
                    department_tax_invoice_id bigserial primary key,
                    department_project_application_id bigint not null,
                    department_registration_id bigint not null,
                    request_id varchar(32) not null,
                    ti_number varchar(50) not null,
                    ti_date date not null,
                    dept_ref_date date not null,
                    project_name varchar(200) not null,
                    project_code varchar(100),
                    pm_name varchar(100),
                    billed_to varchar(200) not null,
                    billing_address varchar(1000) not null,
                    client_gstin_available boolean not null default false,
                    client_gst_number varchar(15),
                    place_of_supply varchar(100) not null,
                    base_amount numeric(14,2) not null,
                    cgst_rate numeric(8,4) not null,
                    cgst_amount numeric(14,2) not null,
                    sgst_rate numeric(8,4) not null,
                    sgst_amount numeric(14,2) not null,
                    tax_amount numeric(14,2) not null,
                    total_amount numeric(14,2) not null,
                    company_name varchar(200) not null,
                    company_address varchar(1000) not null,
                    cin_number varchar(21) not null,
                    pan_number varchar(10) not null,
                    gst_number varchar(15) not null,
                    bank_name varchar(150) not null,
                    branch_name varchar(150) not null,
                    account_holder_name varchar(150) not null,
                    account_number varchar(30) not null,
                    ifsc_code varchar(11) not null,
                    amount_in_words varchar(500) not null,
                    is_active boolean not null default true,
                    created_by varchar(255) not null,
                    created_date timestamp not null,
                    updated_by varchar(255) not null,
                    updated_date timestamp not null,
                    constraint uk_dep_tax_invoice_request_id unique (request_id),
                    constraint uk_dep_tax_invoice_ti_number unique (ti_number),
                    constraint uk_dep_tax_invoice_application_id unique (department_project_application_id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_request_id "
                        + "on department_tax_invoice (request_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_ti_number "
                        + "on department_tax_invoice (ti_number)");
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_application_id "
                        + "on department_tax_invoice (department_project_application_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_registration_id "
                        + "on department_tax_invoice (department_registration_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_ti_date "
                        + "on department_tax_invoice (ti_date)");

        jdbcTemplate.execute("""
                create table if not exists department_tax_invoice_line_item (
                    department_tax_invoice_line_item_id bigserial primary key,
                    department_tax_invoice_id bigint not null,
                    department_project_resource_requirement_id bigint,
                    line_no integer not null,
                    description varchar(500) not null,
                    sac_hsn varchar(20) not null,
                    quantity integer not null,
                    rate_per_month numeric(14,2) not null,
                    duration_in_months integer not null,
                    total_amount numeric(14,2) not null,
                    constraint uk_dep_tax_invoice_line_item_row unique (department_tax_invoice_id, line_no),
                    constraint fk_dep_tax_invoice_line_item_invoice
                        foreign key (department_tax_invoice_id)
                        references department_tax_invoice(department_tax_invoice_id)
                        on delete cascade
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_line_item_invoice_id "
                        + "on department_tax_invoice_line_item (department_tax_invoice_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_dep_tax_invoice_line_item_requirement_id "
                        + "on department_tax_invoice_line_item (department_project_resource_requirement_id)");
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
