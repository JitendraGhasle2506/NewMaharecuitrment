package com.maharecruitment.gov.in.recruitment.repository;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeePipReportRepositoryTest {
    private LocalContainerEntityManagerFactoryBean factory;
    private jakarta.persistence.EntityManager manager;
    private EmployeePipReportRepository reports;
    @BeforeAll void setup() {
        var data=new DriverManagerDataSource("jdbc:h2:mem:hr_pip_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(data);
        jdbc.execute("create table employee_master (employee_id bigint primary key, full_name varchar, employee_code varchar, status varchar)");
        jdbc.execute("create table employee_pip_plan (id bigint primary key, employee_id bigint, start_date date, end_date date, duration_months int, status varchar, created_date_time timestamp)");
        jdbc.execute("insert into employee_master values (1,'TeamA100X','EMP001','ACTIVE'),(2,'Team_100%','EMP002','ACTIVE'),(3,'Former Employee','EMP003','RESIGNED'),(4,'No PIP','EMP004','ACTIVE')");
        jdbc.execute("""
                insert into employee_pip_plan values
                (100,1,date '2026-10-01',date '2026-10-31',1,'UNDER_REVIEW',timestamp '2026-10-01 09:00:00'),
                (101,2,date '2026-08-01',date '2026-08-31',1,'COMPLETED',timestamp '2026-08-01 09:00:00'),
                (102,3,date '2026-09-01',date '2026-09-30',1,'EXIT_RECORDED',timestamp '2026-09-01 09:00:00'),
                (103,2,date '2026-10-02',date '2026-12-01',2,'UNDER_REVIEW',timestamp '2026-10-02 09:00:00')
                """);
        factory=new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(data);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        factory.afterPropertiesSet(); manager=factory.getObject().createEntityManager();
        reports=new JpaRepositoryFactory(manager).getRepository(EmployeePipReportRepository.class);
    }
    @AfterAll void close() { if(manager!=null)manager.close();if(factory!=null)factory.destroy(); }
    @Test void paginatesAllTeamsAndIncludesExitedEmployeesAndPastPlans() {
        var first=reports.findOverview(null,"%",PageRequest.of(0,2));
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getContent()).extracting(EmployeePipReportRepository.Row::getId).containsExactly(103L,100L);
        assertThat(reports.findOverview(null,"%",PageRequest.of(1,2)).getContent())
                .extracting(EmployeePipReportRepository.Row::getId).containsExactly(102L,101L);
        assertThat(reports.findSummaryById(102L).orElseThrow().getEmployeeStatus()).isEqualTo("RESIGNED");
    }
    @Test void filtersStatusesAndSearchesNamesAndCodes() {
        assertThat(reports.findOverview(Status.COMPLETED,"%",PageRequest.of(0,12)).getContent())
                .extracting(EmployeePipReportRepository.Row::getId).containsExactly(101L);
        assertThat(reports.findOverview(null,"%former%",PageRequest.of(0,12)).getContent())
                .extracting(EmployeePipReportRepository.Row::getId).containsExactly(102L);
        assertThat(reports.findOverview(null,"%emp002%",PageRequest.of(0,12)).getTotalElements()).isEqualTo(2);
    }
    @Test void literalWildcardCharactersCannotBroadenEmployeeSearch() {
        assertThat(reports.findOverview(null,"%team!_100!%%",PageRequest.of(0,12)).getContent())
                .extracting(EmployeePipReportRepository.Row::getEmployeeId).containsOnly(2L).hasSize(2);
    }
    @Test void countsAllPipOutcomesAndReturnsNoSummaryForMissingPlans() {
        var counts=new EnumMap<Status,Long>(Status.class);
        reports.countPlansByStatus().forEach(row->counts.put(row.getStatus(),row.getTotal()));
        assertThat(counts).containsEntry(Status.UNDER_REVIEW,2L).containsEntry(Status.COMPLETED,1L).containsEntry(Status.EXIT_RECORDED,1L);
        assertThat(reports.findSummaryById(999L)).isEmpty();
    }
}