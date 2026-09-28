package com.maharecruitment.gov.in.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeCellAuthorityProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeListProjection;

import jakarta.persistence.EntityManager;

/** Runs the real repository/projections against an isolated, synthetic in-memory database. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeRepositoryEmployeeListIntegrationTest {
    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager manager;
    private EmployeeRepository repository;
    private final Sort sort = Sort.by(Sort.Order.asc("employee.fullName").ignoreCase(),
            Sort.Order.asc("employee.employeeId"));

    @BeforeAll
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:employee_list_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        // Only the columns read by this projection are needed; no application configuration or data is used.
        for (String ddl : List.of(
                "create table agency_master (agency_id bigint primary key, agency_name varchar)",
                "create table manpower_designation_master (designation_id bigint primary key, designation_name varchar)",
                "create table m_cell_master (cell_id bigint primary key, cell_name varchar)",
                "create table users (id bigint primary key, name varchar)",
                "create table employee_master (employee_id bigint primary key, employee_code varchar, full_name varchar, "
                        + "email varchar, designation_id bigint, mahait_onboarding_date date, recruitment_type varchar, agency_id bigint, status varchar)",
                "create table employee_cell_mapping (employee_cell_mapping_id bigint primary key, employee_id bigint unique, cell_id bigint)",
                "create table employee_reporting_mapping (mapping_id bigint primary key, employee_id bigint, manager_employee_id bigint, hod_user_id bigint)",
                "create table cell_reporting_authority_mapping (cell_reporting_authority_mapping_id bigint primary key, cell_id bigint, authority_user_id bigint, authority_level int)")) {
            jdbc.execute(ddl);
        }
        jdbc.update("insert into agency_master values (1, 'Test Agency')");
        jdbc.update("insert into manpower_designation_master values (1, 'Engineer')");
        jdbc.update("insert into m_cell_master values (1, 'Application Cell'), (2, 'Level Two Only Cell')");
        jdbc.update("insert into users values (101, 'Level One'), (102, 'Level Two Alpha'), (103, 'Level Two Beta'), (104, 'Assigned HOD')");
        jdbc.update("""
                insert into employee_master (employee_id, employee_code, full_name, email, designation_id,
                    mahait_onboarding_date, recruitment_type, agency_id, status) values
                (1, 'EMP001', 'Test Duplication', 'one@example.test', 1, date '2026-04-01', 'INTERNAL', 1, 'ACTIVE'),
                (2, 'EMP002', 'Test Duplication', 'two@example.test', 1, date '2026-04-01', 'INTERNAL', 1, 'ACTIVE'),
                (3, 'EMP003', 'External Duplication', 'three@example.test', 1, date '2026-04-01', 'EXTERNAL', 1, 'ACTIVE'),
                (4, 'EMP004', 'Resigned Duplication', 'four@example.test', 1, date '2026-04-01', 'INTERNAL', 1, 'RESIGNED'),
                (5, 'EMP005', 'No Cell', 'five@example.test', null, null, 'INTERNAL', null, 'ACTIVE'),
                (6, 'EMP006', 'Only Level Two', 'six@example.test', 1, null, 'INTERNAL', 1, 'ACTIVE'),
                (10, 'EMP010', 'Assigned Manager', 'manager@example.test', 1, null, 'MAHAIT', null, 'ACTIVE')
                """);
        jdbc.update("insert into employee_cell_mapping values (1, 1, 1), (2, 2, 1), (3, 3, 1), (4, 4, 1), (6, 6, 2)");
        jdbc.update("insert into employee_reporting_mapping values (1, 1, null, 101), (2, 1, 10, 104)");
        jdbc.update("insert into cell_reporting_authority_mapping values (1, 1, 101, 1), (2, 1, 102, 2), (3, 1, 103, 2), (4, 2, 102, 2), (5, 2, 103, 2)");

        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "none",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        factory.afterPropertiesSet();
        manager = factory.getObject().createEntityManager();
        repository = new JpaRepositoryFactory(manager).getRepository(EmployeeRepository.class);
    }

    @AfterAll
    void tearDown() {
        if (manager != null) manager.close();
        if (factory != null) factory.destroy();
    }

    @Test
    void threeCellAuthoritiesDoNotDuplicateEmployeesWithOrWithoutExplicitReporting() {
        Page<EmployeeListProjection> page = search("%DUPLICATION%", PageRequest.of(0, 10, sort));
        assertThat(page.getContent()).extracting(EmployeeListProjection::getEmployeeId).containsExactly(1L, 2L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().getFirst().getReportingMappingId()).isEqualTo(2L);
        assertThat(page.getContent().getFirst().getReportingManagerName()).isEqualTo("Assigned Manager");
        assertThat(page.getContent().getFirst().getReportingHodName()).isEqualTo("Assigned HOD");
        assertThat(page.getContent().getLast().getReportingMappingId()).isNull();
    }

    @Test
    void paginationAndCountReferToEmployeesNotAuthorityMappings() {
        Page<EmployeeListProjection> first = search("%DUPLICATION%", PageRequest.of(0, 1, sort));
        Page<EmployeeListProjection> second = search("%DUPLICATION%", PageRequest.of(1, 1, sort));
        assertThat(first.getContent()).extracting(EmployeeListProjection::getEmployeeId).containsExactly(1L);
        assertThat(second.getContent()).extracting(EmployeeListProjection::getEmployeeId).containsExactly(2L);
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(second.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(search("%DUPLICATION%", Pageable.unpaged(sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(1L, 2L);
    }

    @Test
    void searchMatchesAnyFallbackAuthorityButKeepsExplicitReportingPrecedence() {
        // Both L2 authorities match; this must still produce one employee and an accurate count.
        Page<EmployeeListProjection> page = search("%LEVEL TWO%", PageRequest.of(0, 1, sort));
        assertThat(page.getContent()).extracting(EmployeeListProjection::getEmployeeId).containsExactly(6L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(search("%LEVEL TWO%", PageRequest.of(1, 1, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(2L);
        assertThat(search("%ASSIGNED MANAGER%", PageRequest.of(0, 10, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(1L);
        assertThat(search("%LEVEL ONE%", PageRequest.of(0, 10, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(2L);
    }

    @Test
    void keepsCellSearchTypeStatusAgencyFiltersAndEmployeesWithoutMappings() {
        assertThat(search("%APPLICATION CELL%", PageRequest.of(0, 1, sort)).getTotalElements()).isEqualTo(2);
        assertThat(search("%NO CELL%", PageRequest.of(0, 10, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(5L);
        assertThat(repository.findEmployeeListPageByStatusAndFilters("ACTIVE", "INTERNAL", 999L,
                "%DUPLICATION%", PageRequest.of(0, 10, sort))).isEmpty();
        assertThat(repository.findEmployeeListPageByStatusAndFilters("RESIGNED", "INTERNAL", 1L,
                "%DUPLICATION%", PageRequest.of(0, 10, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(4L);
        assertThat(repository.findEmployeeListPageByStatusAndFilters("ACTIVE", "EXTERNAL", 1L,
                "%DUPLICATION%", PageRequest.of(0, 10, sort)).getContent())
                .extracting(EmployeeListProjection::getEmployeeId).containsExactly(3L);
    }

    @Test
    void bulkAuthorityLookupIncludesAllLevelsAndOnlyRequestedEmployees() {
        assertThat(repository.findCellAuthoritiesForEmployeeList(List.of(2L)))
                .extracting(EmployeeCellAuthorityProjection::getAuthorityName)
                .containsExactly("Level One", "Level Two Alpha", "Level Two Beta");
        assertThat(repository.findCellAuthoritiesForEmployeeList(List.of(6L)))
                .extracting(EmployeeCellAuthorityProjection::getAuthorityName)
                .containsExactly("Level Two Alpha", "Level Two Beta");
        assertThat(repository.findCellAuthoritiesForEmployeeList(List.of(5L))).isEmpty();
    }

    private Page<EmployeeListProjection> search(String pattern, Pageable pageable) {
        return repository.findEmployeeListPageByStatusAndFilters("ACTIVE", "INTERNAL", null, pattern, pageable);
    }
}
