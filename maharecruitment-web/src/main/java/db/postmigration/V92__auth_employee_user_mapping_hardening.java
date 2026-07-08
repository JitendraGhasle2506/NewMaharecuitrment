package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V92__auth_employee_user_mapping_hardening extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection) || !tableExists(connection, "users")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        hardenEmployeeUserMapping(connection, jdbcTemplate);
        hardenAgencyUserMapping(connection, jdbcTemplate);
    }

    private void hardenEmployeeUserMapping(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, "employee_master")) {
            return;
        }

        jdbcTemplate.execute("alter table employee_master add column if not exists user_id bigint");

        jdbcTemplate.execute("""
                update employee_master employee
                set user_id = null
                where user_id is not null
                  and not exists (
                        select 1
                        from users app_user
                        where app_user.id = employee.user_id
                  )
                """);

        jdbcTemplate.execute("""
                with matched_employees as (
                    select
                        employee.employee_id,
                        app_user.id as user_id,
                        row_number() over (
                            partition by app_user.id
                            order by
                                case when upper(trim(coalesce(employee.status, ''))) = 'ACTIVE' then 0 else 1 end,
                                case when employee.pre_onboarding_id is not null then 0 else 1 end,
                                coalesce(employee.mahait_onboarding_date, employee.joining_date) desc nulls last,
                                employee.employee_id desc
                        ) as rn
                    from employee_master employee
                    join users app_user
                      on lower(trim(employee.email)) = lower(trim(app_user.email))
                    where employee.user_id is null
                )
                update employee_master employee
                set user_id = matched.user_id
                from matched_employees matched
                where employee.employee_id = matched.employee_id
                  and matched.rn = 1
                """);

        jdbcTemplate.execute("""
                with ranked_employees as (
                    select
                        employee.employee_id,
                        row_number() over (
                            partition by employee.user_id
                            order by
                                case when upper(trim(coalesce(employee.status, ''))) = 'ACTIVE' then 0 else 1 end,
                                case when employee.pre_onboarding_id is not null then 0 else 1 end,
                                coalesce(employee.mahait_onboarding_date, employee.joining_date) desc nulls last,
                                employee.employee_id desc
                        ) as rn
                    from employee_master employee
                    where employee.user_id is not null
                )
                update employee_master employee
                set user_id = null
                from ranked_employees ranked
                where employee.employee_id = ranked.employee_id
                  and ranked.rn > 1
                """);

        if (!indexExists(connection, "employee_master", "idx_employee_master_user_id")) {
            jdbcTemplate.execute("create index idx_employee_master_user_id on employee_master (user_id)");
        }

        if (!constraintExists(connection, "uk_employee_master_user")) {
            jdbcTemplate.execute("""
                    alter table employee_master
                    add constraint uk_employee_master_user unique (user_id)
                    """);
        }

        if (!constraintExists(connection, "fk_employee_master_user")) {
            jdbcTemplate.execute("""
                    alter table employee_master
                    add constraint fk_employee_master_user
                    foreign key (user_id) references users(id)
                    """);
        }
    }

    private void hardenAgencyUserMapping(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, "user_agency_mapping") || !tableExists(connection, "agency_master")) {
            return;
        }

        jdbcTemplate.execute("""
                delete from user_agency_mapping mapping
                where not exists (
                        select 1
                        from users app_user
                        where app_user.id = mapping.user_id
                    )
                   or not exists (
                        select 1
                        from agency_master agency
                        where agency.agency_id = mapping.agency_id
                    )
                """);

        jdbcTemplate.execute("""
                with agency_matches as (
                    select
                        app_user.id as user_id,
                        agency.agency_id,
                        row_number() over (
                            partition by app_user.id
                            order by
                                case when upper(trim(coalesce(agency.status, ''))) = 'ACTIVE' then 0 else 1 end,
                                agency.agency_id
                        ) as rn
                    from users app_user
                    join users_roles user_role on user_role.user_id = app_user.id
                    join roles role on role.id = user_role.role_id
                    join agency_master agency
                      on lower(trim(agency.official_email)) = lower(trim(app_user.email))
                    where upper(trim(role.name)) = 'ROLE_AGENCY'
                )
                insert into user_agency_mapping (
                    user_id,
                    agency_id,
                    primary_mapping,
                    active,
                    effective_from,
                    created_by,
                    created_date,
                    updated_by,
                    updated_date
                )
                select
                    match.user_id,
                    match.agency_id,
                    case when match.rn = 1 then true else false end,
                    true,
                    current_timestamp,
                    'system-backfill',
                    current_timestamp,
                    'system-backfill',
                    current_timestamp
                from agency_matches match
                where not exists (
                    select 1
                    from user_agency_mapping mapping
                    where mapping.user_id = match.user_id
                      and mapping.agency_id = match.agency_id
                )
                """);

        jdbcTemplate.execute("""
                with ranked_mappings as (
                    select
                        user_agency_mapping_id,
                        row_number() over (
                            partition by user_id, agency_id
                            order by
                                case when active then 0 else 1 end,
                                case when primary_mapping then 0 else 1 end,
                                user_agency_mapping_id
                        ) as rn
                    from user_agency_mapping
                )
                delete from user_agency_mapping mapping
                using ranked_mappings ranked
                where mapping.user_agency_mapping_id = ranked.user_agency_mapping_id
                  and ranked.rn > 1
                """);

        jdbcTemplate.execute("""
                create unique index if not exists uk_user_agency_mapping_user_agency
                on user_agency_mapping (user_id, agency_id)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_user_agency_mapping_user_active
                on user_agency_mapping (user_id, active, primary_mapping)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_user_agency_mapping_agency
                on user_agency_mapping (agency_id)
                """);

        if (!constraintExists(connection, "fk_user_agency_mapping_user")) {
            jdbcTemplate.execute("""
                    alter table user_agency_mapping
                    add constraint fk_user_agency_mapping_user
                    foreign key (user_id) references users(id) on delete cascade
                    """);
        }

        if (!constraintExists(connection, "fk_user_agency_mapping_agency")) {
            jdbcTemplate.execute("""
                    alter table user_agency_mapping
                    add constraint fk_user_agency_mapping_agency
                    foreign key (agency_id) references agency_master(agency_id)
                    """);
        }

        if (columnExists(connection, "users", "is_active")) {
            jdbcTemplate.execute("""
                    update users app_user
                    set is_active = false
                    from user_agency_mapping mapping
                    join agency_master agency on agency.agency_id = mapping.agency_id
                    where app_user.id = mapping.user_id
                      and mapping.active = true
                      and upper(trim(coalesce(agency.status, ''))) <> 'ACTIVE'
                    """);
        }
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

    private boolean columnExists(Connection connection, String tableName, String columnName) {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getColumns(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean constraintExists(Connection connection, String constraintName) {
        try (ResultSet rs = connection.createStatement().executeQuery(
                "select 1 from pg_constraint where conname = '" + constraintName + "'")) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            return false;
        }

        try (ResultSet rs = connection.getMetaData().getIndexInfo(
                null,
                null,
                tableName.toUpperCase(Locale.ROOT),
                false,
                false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
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
