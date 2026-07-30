package db.postmigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V35__auth_user_affiliation_normalization_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)
                || !tableExists(connection, "users")
                || !tableExists(connection, "department_registration_master")
                || !tableExists(connection, "agency_master")) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

        jdbcTemplate.execute("""
                create table if not exists user_profile (
                    user_id bigint primary key,
                    full_name varchar(255) not null,
                    mobile_no varchar(15),
                    created_by varchar(255),
                    created_date timestamp,
                    updated_by varchar(255),
                    updated_date timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists user_department_mapping (
                    user_department_mapping_id bigserial primary key,
                    user_id bigint not null,
                    department_registration_id bigint not null,
                    primary_mapping boolean not null default true,
                    active boolean not null default true,
                    effective_from timestamp,
                    effective_to timestamp,
                    created_by varchar(255),
                    created_date timestamp,
                    updated_by varchar(255),
                    updated_date timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists user_agency_mapping (
                    user_agency_mapping_id bigserial primary key,
                    user_id bigint not null,
                    agency_id bigint not null,
                    primary_mapping boolean not null default true,
                    active boolean not null default true,
                    effective_from timestamp,
                    effective_to timestamp,
                    created_by varchar(255),
                    created_date timestamp,
                    updated_by varchar(255),
                    updated_date timestamp
                )
                """);

        jdbcTemplate.execute("""
                create index if not exists idx_user_profile_mobile on user_profile (mobile_no)
                """);
        jdbcTemplate.execute("""
                create unique index if not exists uk_user_department_mapping_user_department
                on user_department_mapping (user_id, department_registration_id)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_user_department_mapping_user_active
                on user_department_mapping (user_id, active, primary_mapping)
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_user_department_mapping_department
                on user_department_mapping (department_registration_id)
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

        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_user_profile_user'
                    ) then
                        alter table user_profile
                            add constraint fk_user_profile_user
                            foreign key (user_id) references users(id) on delete cascade;
                    end if;
                end
                $$;
                """);
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_user_department_mapping_user'
                    ) then
                        alter table user_department_mapping
                            add constraint fk_user_department_mapping_user
                            foreign key (user_id) references users(id) on delete cascade;
                    end if;
                end
                $$;
                """);
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_user_department_mapping_department'
                    ) then
                        alter table user_department_mapping
                            add constraint fk_user_department_mapping_department
                            foreign key (department_registration_id)
                            references department_registration_master(department_registration_id);
                    end if;
                end
                $$;
                """);
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_user_agency_mapping_user'
                    ) then
                        alter table user_agency_mapping
                            add constraint fk_user_agency_mapping_user
                            foreign key (user_id) references users(id) on delete cascade;
                    end if;
                end
                $$;
                """);
        jdbcTemplate.execute("""
                do $$
                begin
                    if not exists (
                        select 1
                        from pg_constraint
                        where conname = 'fk_user_agency_mapping_agency'
                    ) then
                        alter table user_agency_mapping
                            add constraint fk_user_agency_mapping_agency
                            foreign key (agency_id) references agency_master(agency_id);
                    end if;
                end
                $$;
                """);

        jdbcTemplate.execute("""
                insert into user_profile (
                    user_id,
                    full_name,
                    mobile_no,
                    created_by,
                    created_date,
                    updated_by,
                    updated_date
                )
                select
                    u.id,
                    coalesce(nullif(trim(u.name), ''), trim(u.email)),
                    nullif(trim(u.mobile_no), ''),
                    'system-backfill',
                    current_timestamp,
                    'system-backfill',
                    current_timestamp
                from users u
                where not exists (
                    select 1
                    from user_profile up
                    where up.user_id = u.id
                )
                """);
        jdbcTemplate.execute("""
                insert into user_department_mapping (
                    user_id,
                    department_registration_id,
                    primary_mapping,
                    active,
                    effective_from,
                    created_by,
                    created_date,
                    updated_by,
                    updated_date
                )
                select
                    u.id,
                    u.department_registration_id,
                    true,
                    true,
                    current_timestamp,
                    'system-backfill',
                    current_timestamp,
                    'system-backfill',
                    current_timestamp
                from users u
                where u.department_registration_id is not null
                  and not exists (
                    select 1
                    from user_department_mapping udm
                    where udm.user_id = u.id
                      and udm.department_registration_id = u.department_registration_id
                )
                """);
        jdbcTemplate.execute("""
                with agency_matches as (
                    select
                        u.id as user_id,
                        a.agency_id,
                        row_number() over (partition by u.id order by a.agency_id) as rn
                    from users u
                    join users_roles ur on ur.user_id = u.id
                    join roles r on r.id = ur.role_id
                    join agency_master a
                      on lower(trim(a.official_email)) = lower(trim(u.email))
                    where upper(trim(r.name)) = 'ROLE_AGENCY'
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
                    from user_agency_mapping uam
                    where uam.user_id = match.user_id
                      and uam.agency_id = match.agency_id
                )
                """);
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
