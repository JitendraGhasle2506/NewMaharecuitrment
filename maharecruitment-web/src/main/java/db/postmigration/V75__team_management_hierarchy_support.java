package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V75__team_management_hierarchy_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    alter table project_mst
                    add column if not exists project_code varchar(30)
                    """);

            statement.execute("""
                    update project_mst
                    set project_code = 'PRJ-' || project_id
                    where project_code is null or trim(project_code) = ''
                    """);

            statement.execute("""
                    create unique index if not exists ux_project_mst_project_code_lower
                    on project_mst (lower(project_code))
                    where project_code is not null
                    """);

            statement.execute("""
                    create table if not exists team_master (
                        team_id bigserial primary key,
                        team_name varchar(150) not null,
                        team_type varchar(30) not null,
                        parent_team_id bigint,
                        project_id bigint,
                        cell_id bigint not null,
                        display_order integer not null default 0,
                        status varchar(20) not null default 'ACTIVE',
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint uk_team_master_cell_team unique (cell_id, team_name),
                        constraint fk_team_master_project foreign key (project_id) references project_mst(project_id),
                        constraint fk_team_master_cell foreign key (cell_id) references m_cell_master(cell_id),
                        constraint fk_team_master_parent foreign key (parent_team_id) references team_master(team_id),
                        constraint ck_team_master_type check (team_type in ('DEVELOPMENT', 'OM', 'SUPPORT')),
                        constraint ck_team_master_status check (status in ('ACTIVE', 'INACTIVE'))
                    )
                    """);

            statement.execute("""
                    alter table team_master
                    add column if not exists cell_id bigint
                    """);

            statement.execute("""
                    update team_master team
                    set cell_id = project.cell_id
                    from project_mst project
                    where team.project_id = project.project_id
                      and team.cell_id is null
                    """);

            statement.execute("""
                    update team_master
                    set cell_id = (
                        select cell_id
                        from m_cell_master
                        where active_flag = 'Y'
                        order by cell_id
                        fetch first 1 row only
                    )
                    where cell_id is null
                      and exists (select 1 from m_cell_master where active_flag = 'Y')
                    """);

            statement.execute("""
                    alter table team_master
                    alter column project_id drop not null
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (select 1 from team_master where cell_id is null) then
                            alter table team_master
                            alter column cell_id set not null;
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    alter table team_master
                    drop constraint if exists uk_team_master_project_team
                    """);

            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'uk_team_master_cell_team'
                              and table_name = 'team_master'
                        )
                        and not exists (
                            select 1
                            from team_master
                            where cell_id is not null
                            group by cell_id, team_name
                            having count(*) > 1
                        ) then
                            alter table team_master
                            add constraint uk_team_master_cell_team unique (cell_id, team_name);
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    create index if not exists idx_team_master_project_status
                    on team_master (project_id, status)
                    """);
            statement.execute("""
                    create index if not exists idx_team_master_cell_id
                    on team_master (cell_id)
                    """);
            statement.execute("""
                    create index if not exists idx_team_master_parent
                    on team_master (parent_team_id)
                    """);
            statement.execute("""
                    create index if not exists idx_team_master_type
                    on team_master (team_type)
                    """);
            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from pg_indexes
                            where schemaname = current_schema()
                              and indexname = 'ux_team_master_cell_team_lower'
                        )
                        and not exists (
                            select 1
                            from team_master
                            where cell_id is not null
                            group by cell_id, lower(team_name)
                            having count(*) > 1
                        ) then
                            execute 'create unique index ux_team_master_cell_team_lower on team_master (cell_id, lower(team_name))';
                        end if;
                    end $$;
                    """);
            statement.execute("""
                    do $$
                    begin
                        if not exists (
                            select 1
                            from information_schema.table_constraints
                            where constraint_name = 'fk_team_master_cell'
                              and table_name = 'team_master'
                        ) then
                            alter table team_master
                            add constraint fk_team_master_cell
                            foreign key (cell_id) references m_cell_master(cell_id);
                        end if;
                    end $$;
                    """);

            statement.execute("""
                    create table if not exists position_master (
                        position_id bigserial primary key,
                        position_name varchar(150) not null,
                        project_id bigint,
                        team_id bigint,
                        designation_id bigint not null,
                        level_code varchar(10),
                        reporting_position_id bigint,
                        employee_id bigint,
                        display_order integer not null default 0,
                        position_status varchar(20) not null default 'VACANT',
                        status varchar(20) not null default 'ACTIVE',
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_position_master_project foreign key (project_id) references project_mst(project_id),
                        constraint fk_position_master_team foreign key (team_id) references team_master(team_id),
                        constraint fk_position_master_designation foreign key (designation_id)
                            references manpower_designation_master(designation_id),
                        constraint fk_position_master_level foreign key (level_code)
                            references resource_level_experience_mst(level_code),
                        constraint fk_position_master_reporting foreign key (reporting_position_id)
                            references position_master(position_id),
                        constraint fk_position_master_employee foreign key (employee_id) references employee_master(employee_id),
                        constraint ck_position_master_status check (status in ('ACTIVE', 'INACTIVE')),
                        constraint ck_position_master_position_status check (position_status in ('FILLED', 'VACANT'))
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_position_master_project_status
                    on position_master (project_id, status)
                    """);
            statement.execute("""
                    create index if not exists idx_position_master_team
                    on position_master (team_id)
                    """);
            statement.execute("""
                    create index if not exists idx_position_master_level
                    on position_master (level_code)
                    """);
            statement.execute("""
                    create index if not exists idx_position_master_reporting
                    on position_master (reporting_position_id)
                    """);
            statement.execute("""
                    create index if not exists idx_position_master_employee
                    on position_master (employee_id)
                    """);
            statement.execute("""
                    create index if not exists idx_position_master_position_status
                    on position_master (position_status)
                    """);
            statement.execute("""
                    create unique index if not exists ux_position_master_active_employee
                    on position_master (employee_id)
                    where employee_id is not null and status = 'ACTIVE'
                    """);

            statement.execute("""
                    create table if not exists employee_team_mapping (
                        mapping_id bigserial primary key,
                        employee_id bigint,
                        team_id bigint not null,
                        position_id bigint not null,
                        effective_date date not null default current_date,
                        status varchar(20) not null default 'ACTIVE',
                        created_date_time timestamp not null default current_timestamp,
                        updated_date_time timestamp not null default current_timestamp,
                        constraint fk_employee_team_mapping_employee foreign key (employee_id)
                            references employee_master(employee_id),
                        constraint fk_employee_team_mapping_team foreign key (team_id) references team_master(team_id),
                        constraint fk_employee_team_mapping_position foreign key (position_id)
                            references position_master(position_id),
                        constraint ck_employee_team_mapping_status check (status in ('ACTIVE', 'INACTIVE'))
                    )
                    """);

            statement.execute("""
                    create index if not exists idx_employee_team_mapping_employee
                    on employee_team_mapping (employee_id)
                    """);
            statement.execute("""
                    create index if not exists idx_employee_team_mapping_team_status
                    on employee_team_mapping (team_id, status)
                    """);
            statement.execute("""
                    create index if not exists idx_employee_team_mapping_position_status
                    on employee_team_mapping (position_id, status)
                    """);
            statement.execute("""
                    create index if not exists idx_employee_team_mapping_effective
                    on employee_team_mapping (effective_date)
                    """);
            statement.execute("""
                    create unique index if not exists ux_employee_team_mapping_active_position
                    on employee_team_mapping (position_id)
                    where status = 'ACTIVE'
                    """);

            statement.execute("""
                    create table if not exists team_management_audit_log (
                        audit_id bigserial primary key,
                        action_type varchar(50) not null,
                        entity_type varchar(50) not null,
                        entity_id varchar(80) not null,
                        actor_login_id varchar(255),
                        summary varchar(255) not null,
                        details text,
                        occurred_at timestamp not null default current_timestamp
                    )
                    """);
            statement.execute("""
                    create index if not exists idx_team_mgmt_audit_entity
                    on team_management_audit_log (entity_type, entity_id)
                    """);
            statement.execute("""
                    create index if not exists idx_team_mgmt_audit_action
                    on team_management_audit_log (action_type)
                    """);
            statement.execute("""
                    create index if not exists idx_team_mgmt_audit_occurred
                    on team_management_audit_log (occurred_at)
                    """);

            syncExistingSequences(statement);
            seedSampleData(statement);
        }
    }

    private void syncExistingSequences(Statement statement) throws Exception {
        syncSequence(statement, "m_wing_master", "wing_id");
        syncSequence(statement, "m_cell_master", "cell_id");
        syncSequence(statement, "project_mst", "project_id");
        syncSequence(statement, "manpower_designation_master", "designation_id");
    }

    private void syncSequence(Statement statement, String tableName, String columnName) throws Exception {
        statement.execute(
                "do $$ "
                        + "declare sequence_name text; max_id bigint; "
                        + "begin "
                        + "sequence_name := pg_get_serial_sequence('" + tableName + "', '" + columnName + "'); "
                        + "if sequence_name is not null then "
                        + "execute 'select max(" + columnName + ") from " + tableName + "' into max_id; "
                        + "if max_id is null then "
                        + "perform setval(sequence_name, 1, false); "
                        + "else "
                        + "perform setval(sequence_name, max_id, true); "
                        + "end if; "
                        + "end if; "
                        + "end $$");
    }

    private void seedSampleData(Statement statement) throws Exception {
        statement.execute("""
                insert into m_wing_master (wing_name, active_flag, created_date_time, updated_date_time)
                select 'MAHAIT Project Cells', 'Y', current_timestamp, current_timestamp
                where not exists (
                    select 1 from m_wing_master where lower(wing_name) = lower('MAHAIT Project Cells')
                )
                """);

        statement.execute("""
                insert into m_cell_master (cell_name, wing_id, active_flag, created_date_time, updated_date_time)
                select 'Recruitment Project Cell', wing.wing_id, 'Y', current_timestamp, current_timestamp
                from m_wing_master wing
                where lower(wing.wing_name) = lower('MAHAIT Project Cells')
                  and not exists (
                    select 1 from m_cell_master where lower(cell_name) = lower('Recruitment Project Cell')
                  )
                """);

        statement.execute("""
                insert into project_mst (
                    project_name, project_code, project_desc, project_type, project_scope_type,
                    cell_id, active_flag, created_date_time, updated_date_time
                )
                select 'MAHAIT Recruitment Cell', 'MRC', 'Sample MAHAIT organization hierarchy',
                       'NEW_DEVELOPMENT', 'INTERNAL', cell.cell_id, 'Y', current_timestamp, current_timestamp
                from m_cell_master cell
                where lower(cell.cell_name) = lower('Recruitment Project Cell')
                  and not exists (
                    select 1 from project_mst where lower(project_code) = lower('MRC')
                  )
                """);

        seedDesignation(statement, "Senior Technical Manager (STM)");
        seedDesignation(statement, "Project Manager");
        seedDesignation(statement, "Project Lead");
        seedDesignation(statement, "SSD");
        seedDesignation(statement, "SD");
        seedDesignation(statement, "QM");
        seedDesignation(statement, "QAL");

        seedTeam(statement, "Development Team", "DEVELOPMENT", null, 10);
        seedTeam(statement, "Team D-15", "DEVELOPMENT", "Development Team", 15);
        seedTeam(statement, "Team D-16", "DEVELOPMENT", "Development Team", 16);
        seedTeam(statement, "O&M Team", "OM", null, 20);
        seedTeam(statement, "Team O-4", "OM", "O&M Team", 24);
        seedTeam(statement, "Support Team", "SUPPORT", null, 30);
        seedTeam(statement, "Team SQA", "SUPPORT", "Support Team", 31);

        seedPosition(statement, "Senior Technical Manager", "Senior Technical Manager (STM)", null, null, null, 1);
        seedPosition(statement, "Project Manager", "Project Manager", null, "Senior Technical Manager", null, 2);
        seedPosition(statement, "Development Project Lead", "Project Lead", "Development Team", "Project Manager", null, 10);
        seedPosition(statement, "O&M Project Lead", "Project Lead", "O&M Team", "Project Manager", null, 20);
        seedPosition(statement, "Team D-15 Lead", "SSD", "Team D-15", "Development Project Lead", "Gajanan Thakare", 151);
        seedPosition(statement, "Team D-15 Developer 1", "SD", "Team D-15", "Team D-15 Lead", null, 152);
        seedPosition(statement, "Team D-15 Developer 2", "SD", "Team D-15", "Team D-15 Lead", null, 153);
        seedPosition(statement, "Team D-16 Lead", "SSD", "Team D-16", "Development Project Lead", null, 161);
        seedPosition(statement, "Team D-16 Developer", "SD", "Team D-16", "Team D-16 Lead", null, 162);
        seedPosition(statement, "Team O-4 Developer", "SD", "Team O-4", "O&M Project Lead", "Kiran Jadhav", 241);
        seedPosition(statement, "Team O-4 Developer 2", "SD", "Team O-4", "Team O-4 Developer", null, 242);
        seedPosition(statement, "Team SQA Manager", "QM", "Team SQA", "Project Manager", "Mallikarjun Kopuri", 311);
        seedPosition(statement, "Team SQA Lead", "QAL", "Team SQA", "Team SQA Manager", null, 312);

        statement.execute("""
                insert into employee_team_mapping (
                    employee_id, team_id, position_id, effective_date, status, created_date_time, updated_date_time
                )
                select pos.employee_id, pos.team_id, pos.position_id, current_date, 'ACTIVE',
                       current_timestamp, current_timestamp
                from position_master pos
                join project_mst project on project.project_id = pos.project_id
                where project.project_code = 'MRC'
                  and pos.team_id is not null
                  and pos.employee_id is not null
                  and not exists (
                    select 1 from employee_team_mapping existing
                    where existing.position_id = pos.position_id
                      and existing.status = 'ACTIVE'
                  )
                """);
    }

    private void seedDesignation(Statement statement, String designationName) throws Exception {
        String designation = escapeSql(designationName);
        statement.execute(
                "insert into manpower_designation_master ("
                        + "category, designation_name, role_name, active_flag, created_date_time, updated_date_time"
                        + ") "
                        + "select 'MAHAIT Organization', '" + designation + "', '" + designation + "', "
                        + "'Y', current_timestamp, current_timestamp "
                        + "where not exists ("
                        + "select 1 from manpower_designation_master "
                        + "where lower(designation_name) = lower('" + designation + "') "
                        + "and active_flag = 'Y'"
                        + ")");
    }

    private void seedTeam(
            Statement statement,
            String teamName,
            String teamType,
            String parentTeamName,
            int displayOrder) throws Exception {
                        String parentSelect = parentTeamName == null
                ? "null"
                : "(select parent.team_id from team_master parent "
                        + "join project_mst parent_project on parent_project.cell_id = parent.cell_id "
                        + "where parent_project.project_code = 'MRC' "
                        + "and lower(parent.team_name) = lower('" + escapeSql(parentTeamName) + "') "
                        + "fetch first 1 row only)";
        String team = escapeSql(teamName);
        statement.execute(
                "insert into team_master ("
                        + "project_id, cell_id, team_name, team_type, parent_team_id, display_order, status, "
                        + "created_date_time, updated_date_time"
                        + ") "
                        + "select project.project_id, project.cell_id, '" + team + "', '" + teamType + "', "
                        + parentSelect + ", " + displayOrder + ", 'ACTIVE', "
                        + "current_timestamp, current_timestamp "
                        + "from project_mst project "
                        + "where project.project_code = 'MRC' "
                        + "and not exists ("
                        + "select 1 from team_master existing "
                        + "where existing.cell_id = project.cell_id "
                        + "and lower(existing.team_name) = lower('" + team + "')"
                        + ")");
    }

    private void seedPosition(
            Statement statement,
            String positionName,
            String designationName,
            String teamName,
            String reportingPositionName,
            String employeeName,
            int displayOrder) throws Exception {
        String teamSelect = teamName == null
                ? "null"
                : "(select team.team_id from team_master team "
                        + "join project_mst team_project on team_project.cell_id = team.cell_id "
                        + "where team_project.project_code = 'MRC' "
                        + "and lower(team.team_name) = lower('" + escapeSql(teamName) + "') "
                        + "fetch first 1 row only)";
        String reportingSelect = reportingPositionName == null
                ? "null"
                : "(select reporting.position_id from position_master reporting "
                        + "join project_mst reporting_project on reporting_project.project_id = reporting.project_id "
                        + "where reporting_project.project_code = 'MRC' "
                        + "and lower(reporting.position_name) = lower('" + escapeSql(reportingPositionName) + "') "
                        + "fetch first 1 row only)";
        String employeeSelect = employeeName == null
                ? "null"
                : "(select employee.employee_id from employee_master employee "
                        + "where lower(employee.full_name) = lower('" + escapeSql(employeeName) + "') "
                        + "and upper(employee.status) = 'ACTIVE' "
                        + "fetch first 1 row only)";
        String position = escapeSql(positionName);
        String designation = escapeSql(designationName);
        statement.execute(
                "insert into position_master ("
                        + "position_name, project_id, team_id, designation_id, reporting_position_id, employee_id, "
                        + "display_order, position_status, status, created_date_time, updated_date_time"
                        + ") "
                        + "select '" + position + "', project.project_id, " + teamSelect + ", "
                        + "designation.designation_id, " + reportingSelect + ", " + employeeSelect + ", "
                        + displayOrder + ", "
                        + "case when " + employeeSelect + " is null then 'VACANT' else 'FILLED' end, "
                        + "'ACTIVE', current_timestamp, current_timestamp "
                        + "from project_mst project "
                        + "join manpower_designation_master designation "
                        + "on lower(designation.designation_name) = lower('" + designation + "') "
                        + "and designation.active_flag = 'Y' "
                        + "where project.project_code = 'MRC' "
                        + "and not exists ("
                        + "select 1 from position_master existing "
                        + "where existing.project_id = project.project_id "
                        + "and lower(existing.position_name) = lower('" + position + "')"
                        + ")");
    }

    private String escapeSql(String value) {
        return value == null ? null : value.replace("'", "''");
    }
}
