package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V89__employee_profile_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "employee_master")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbcTemplate.execute("alter table employee_master add column if not exists photo_path varchar(1000)");
        jdbcTemplate.execute("""
                create table if not exists employee_profile (
                    id bigserial primary key,
                    employee_id bigint not null,
                    dob date,
                    gender varchar(20),
                    alternate_mobile_no varchar(15),
                    pan_no varchar(10),
                    marital_status varchar(30),
                    blood_group varchar(10),
                    emergency_contact_name varchar(100),
                    emergency_contact_no varchar(15),
                    current_address varchar(1000),
                    permanent_address varchar(1000),
                    photo_path varchar(1000),
                    created_date timestamp not null default current_timestamp,
                    updated_date timestamp not null default current_timestamp,
                    created_by varchar(255),
                    updated_by varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                do $$
                begin
                    if exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'user_id'
                          and is_nullable = 'NO'
                    ) then
                        alter table employee_profile alter column user_id drop not null;
                    end if;
                    if exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'full_name'
                          and is_nullable = 'NO'
                    ) then
                        alter table employee_profile alter column full_name drop not null;
                    end if;
                    if exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'email'
                          and is_nullable = 'NO'
                    ) then
                        alter table employee_profile alter column email drop not null;
                    end if;
                    if exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'current_address'
                          and is_nullable = 'NO'
                    ) then
                        alter table employee_profile alter column current_address drop not null;
                    end if;
                    if exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'permanent_address'
                          and is_nullable = 'NO'
                    ) then
                        alter table employee_profile alter column permanent_address drop not null;
                    end if;
                    alter table employee_profile add column if not exists employee_id bigint;
                    alter table employee_profile add column if not exists dob date;
                    alter table employee_profile add column if not exists gender varchar(20);
                    update employee_profile
                    set dob = null
                    where dob = date '1900-01-01';
                    update employee_profile
                    set gender = null
                    where upper(trim(coalesce(gender, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set pan_no = null
                    where upper(trim(coalesce(pan_no, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set blood_group = null
                    where upper(trim(coalesce(blood_group, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set emergency_contact_name = null
                    where upper(trim(coalesce(emergency_contact_name, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set emergency_contact_no = null
                    where upper(trim(coalesce(emergency_contact_no, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set current_address = null
                    where upper(trim(coalesce(current_address, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    update employee_profile
                    set permanent_address = null
                    where upper(trim(coalesce(permanent_address, ''))) in ('NOT_SPECIFIED', 'NOT_PROVIDED');
                    if exists (
                        select 1 from information_schema.tables where table_name = 'users'
                    ) and exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'user_id'
                    ) then
                        update employee_profile profile
                        set employee_id = employee.employee_id
                        from users app_user
                        join employee_master employee
                          on upper(trim(employee.email)) = upper(trim(app_user.email))
                        where profile.employee_id is null
                          and profile.user_id = app_user.id;
                    end if;
                    if not exists (
                        select 1 from pg_constraint where conname = 'uk_employee_profile_employee'
                    ) then
                        alter table employee_profile
                        add constraint uk_employee_profile_employee unique (employee_id);
                    end if;
                    if not exists (
                        select 1 from pg_constraint where conname = 'fk_employee_profile_employee'
                    ) then
                        alter table employee_profile
                        add constraint fk_employee_profile_employee
                        foreign key (employee_id) references employee_master (employee_id);
                    end if;
                    if not exists (
                        select 1 from information_schema.columns
                        where table_name = 'employee_profile'
                          and column_name = 'employee_id'
                          and is_nullable = 'YES'
                    ) then
                        null;
                    elsif not exists (
                        select 1 from employee_profile where employee_id is null
                    ) then
                        alter table employee_profile alter column employee_id set not null;
                    end if;
                    alter table employee_profile drop constraint if exists chk_employee_profile_pan;
                    alter table employee_profile
                    add constraint chk_employee_profile_pan
                    check (pan_no is null or pan_no = '' or pan_no ~ '^[A-Z]{5}[0-9]{4}[A-Z]$');
                end
                $$;
                """);
        jdbcTemplate.execute("create index if not exists idx_employee_profile_employee_id on employee_profile (employee_id)");
    }

    private boolean tableExists(Connection connection, String tableName) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
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
