package db.postmigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationActivityType;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

public class R__department_and_recruitment_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        ensureRecruitmentInterviewColumns(connection, jdbcTemplate);
        ensureDepartmentConstraints(connection, jdbcTemplate);
        ensureInternalVacancyOpeningSchema(connection, jdbcTemplate);
    }

    private void ensureRecruitmentInterviewColumns(Connection connection, JdbcTemplate jdbcTemplate) {
        String tableName = "recruitment_interview_detail";
        if (!tableExists(connection, tableName)) {
            return;
        }

        ensureColumn(connection, jdbcTemplate, tableName, "department_interview_change_requested", "boolean default false");
        ensureColumn(connection, jdbcTemplate, tableName, "department_interview_change_reason", "varchar(1000)");
        ensureColumn(connection, jdbcTemplate, tableName, "department_interview_change_requested_at", "timestamp");
        ensureColumn(connection, jdbcTemplate, tableName, "department_interview_change_requested_by_user_id", "bigint");
        ensureColumn(connection, jdbcTemplate, tableName, "assessment_submitted", "boolean default false");
        ensureColumn(connection, jdbcTemplate, tableName, "assessment_submitted_at", "timestamp");
        ensureColumn(connection, jdbcTemplate, tableName, "assessment_submitted_by_user_id", "bigint");
        ensureColumn(connection, jdbcTemplate, tableName, "final_decision_status", "varchar(20)");
        ensureColumn(connection, jdbcTemplate, tableName, "final_decision_at", "timestamp");
        ensureColumn(connection, jdbcTemplate, tableName, "final_decision_by_user_id", "bigint");
        ensureColumn(connection, jdbcTemplate, tableName, "final_decision_remarks", "varchar(1000)");

        jdbcTemplate.execute(
                "update recruitment_interview_detail "
                        + "set assessment_submitted = case when assessment_submitted_at is not null then true else false end "
                        + "where assessment_submitted is null");
        jdbcTemplate.execute(
                "alter table recruitment_interview_detail alter column assessment_submitted set default false");
        jdbcTemplate.execute(
                "alter table recruitment_interview_detail alter column assessment_submitted set not null");
    }

    private void ensureDepartmentConstraints(Connection connection, JdbcTemplate jdbcTemplate) {
        if (!tableExists(connection, "department_project_application")
                || !tableExists(connection, "department_project_application_activity")) {
            return;
        }

        List<String> allowedStatuses = merge(
                List.of(
                        "DRAFT", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED",
                        "SUBMITTED_TO_HR", "HR_SENT_BACK", "CORRECTED_BY_DEPARTMENT", "HR_APPROVED",
                        "HR_REJECTED", "AUDITOR_REVIEW", "AUDITOR_SENT_BACK", "AUDITOR_APPROVED", "COMPLETED"),
                Stream.of(DepartmentApplicationStatus.values()).map(Enum::name).toList());

        List<String> allowedActivities = Stream.of(DepartmentApplicationActivityType.values())
                .map(Enum::name)
                .toList();

        updateConstraint(jdbcTemplate,
                "department_project_application",
                "application_status",
                "department_project_application_application_status_check",
                allowedStatuses);
        updateConstraint(jdbcTemplate,
                "department_project_application_activity",
                "previous_status",
                "department_project_application_activity_previous_status_check",
                allowedStatuses);
        updateConstraint(jdbcTemplate,
                "department_project_application_activity",
                "new_status",
                "department_project_application_activity_new_status_check",
                allowedStatuses);
        updateConstraint(jdbcTemplate,
                "department_project_application_activity",
                "activity_type",
                "department_project_application_activity_activity_type_check",
                allowedActivities);
    }

    private void ensureInternalVacancyOpeningSchema(Connection connection, JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table if not exists recruitment_request_sequence (
                    recruitment_request_sequence_id bigserial primary key,
                    sequence_date date not null,
                    request_type_code varchar(1) not null,
                    last_sequence integer not null default 0,
                    constraint uk_recruitment_request_sequence_date_type
                        unique (sequence_date, request_type_code)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_recruitment_request_sequence_date_type "
                        + "on recruitment_request_sequence (sequence_date, request_type_code)");

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_opening (
                    internal_vacancy_opening_id bigserial primary key,
                    request_id varchar(32) not null,
                    project_id bigint not null,
                    status varchar(20) not null,
                    remarks varchar(1000),
                    created_by_email varchar(255) not null,
                    updated_by_email varchar(255) not null,
                    created_date_time timestamp not null default current_timestamp,
                    updated_date_time timestamp not null default current_timestamp,
                    constraint uk_internal_vacancy_opening_request_id unique (request_id),
                    constraint fk_internal_vacancy_opening_project
                        foreign key (project_id) references project_mst(project_id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_opening_request_id "
                        + "on internal_vacancy_opening (request_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_opening_project "
                        + "on internal_vacancy_opening (project_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_opening_status "
                        + "on internal_vacancy_opening (status)");

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_opening_requirement (
                    internal_vacancy_opening_requirement_id bigserial primary key,
                    internal_vacancy_opening_id bigint not null,
                    designation_id bigint not null,
                    level_code varchar(10) not null,
                    monthly_rate numeric(14,2) not null,
                    number_of_vacancy bigint not null,
                    filled_positions bigint not null default 0,
                    constraint uk_internal_vacancy_opening_requirement_row
                        unique (internal_vacancy_opening_id, designation_id, level_code),
                    constraint fk_internal_vacancy_opening_requirement_opening
                        foreign key (internal_vacancy_opening_id)
                        references internal_vacancy_opening(internal_vacancy_opening_id)
                        on delete cascade,
                    constraint fk_internal_vacancy_opening_requirement_designation
                        foreign key (designation_id) references manpower_designation_master(designation_id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_opening_requirement_opening "
                        + "on internal_vacancy_opening_requirement (internal_vacancy_opening_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_opening_requirement_designation "
                        + "on internal_vacancy_opening_requirement (designation_id)");

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_interview_role (
                    internal_vacancy_interview_role_id bigserial primary key,
                    internal_vacancy_opening_id bigint not null,
                    role_id bigint not null,
                    created_date_time timestamp not null default current_timestamp,
                    updated_date_time timestamp not null default current_timestamp,
                    constraint uk_internal_vacancy_interview_role
                        unique (internal_vacancy_opening_id, role_id),
                    constraint fk_internal_vacancy_interview_role_opening
                        foreign key (internal_vacancy_opening_id)
                        references internal_vacancy_opening(internal_vacancy_opening_id)
                        on delete cascade,
                    constraint fk_internal_vacancy_interview_role_role
                        foreign key (role_id) references roles(id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_role_opening "
                        + "on internal_vacancy_interview_role (internal_vacancy_opening_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_role_role "
                        + "on internal_vacancy_interview_role (role_id)");

        jdbcTemplate.execute("""
                create table if not exists internal_vacancy_interview_authority (
                    internal_vacancy_interview_authority_id bigserial primary key,
                    internal_vacancy_opening_id bigint not null,
                    user_id bigint not null,
                    created_date_time timestamp not null default current_timestamp,
                    updated_date_time timestamp not null default current_timestamp,
                    constraint uk_internal_vacancy_interview_authority
                        unique (internal_vacancy_opening_id, user_id),
                    constraint fk_internal_vacancy_interview_authority_opening
                        foreign key (internal_vacancy_opening_id)
                        references internal_vacancy_opening(internal_vacancy_opening_id)
                        on delete cascade,
                    constraint fk_internal_vacancy_interview_authority_user
                        foreign key (user_id) references users(id)
                )
                """);
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_authority_opening "
                        + "on internal_vacancy_interview_authority (internal_vacancy_opening_id)");
        jdbcTemplate.execute(
                "create index if not exists idx_internal_vacancy_interview_authority_user "
                        + "on internal_vacancy_interview_authority (user_id)");

        if (tableExists(connection, "internal_vacancy_opening")) {
            List<String> allowedStatuses = Stream.of(InternalVacancyOpeningStatus.values())
                    .map(Enum::name)
                    .toList();
            updateConstraint(
                    jdbcTemplate,
                    "internal_vacancy_opening",
                    "status",
                    "internal_vacancy_opening_status_check",
                    allowedStatuses);
        }
    }

    private List<String> merge(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        Stream.concat(first.stream(), second.stream())
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .forEach(merged::add);
        return List.copyOf(merged);
    }

    private void updateConstraint(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String columnName,
            String constraintName,
            List<String> values) {
        String valueSql = values.stream()
                .map(value -> "'" + value.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("alter table " + tableName + " drop constraint if exists " + constraintName);
        jdbcTemplate.execute("alter table " + tableName + " add constraint " + constraintName
                + " check (" + columnName + " in (" + valueSql + "))");
    }

    private void ensureColumn(
            Connection connection,
            JdbcTemplate jdbcTemplate,
            String tableName,
            String columnName,
            String definition) {
        if (!columnExists(connection, tableName, columnName)) {
            jdbcTemplate.execute("alter table " + tableName + " add column " + columnName + " " + definition);
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
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            try (ResultSet columns = metaData.getColumns(null, schema, tableName, columnName)) {
                if (columns.next()) {
                    return true;
                }
            }
            try (ResultSet columns = metaData.getColumns(
                    null,
                    schema != null ? schema.toUpperCase(Locale.ROOT) : null,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT))) {
                return columns.next();
            }
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
