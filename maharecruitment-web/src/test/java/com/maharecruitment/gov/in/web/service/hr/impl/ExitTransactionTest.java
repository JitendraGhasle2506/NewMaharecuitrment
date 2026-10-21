package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.maharecruitment.gov.in.audit.entity.AuditEventEntity;
import com.maharecruitment.gov.in.audit.repository.AuditEventRepository;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.audit.service.impl.AuditTrailServiceImpl;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserService;
import com.maharecruitment.gov.in.common.audit.AuditRequestContext;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;

/** Synthetic H2 persistence exercises the real service and audit transaction boundaries. */
class ExitTransactionTest {
    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableMethodSecurity
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:exit_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean EmployeeRepository employees() { return mock(EmployeeRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean RecruitmentDesignationVacancyRepository vacancies() { return mock(RecruitmentDesignationVacancyRepository.class); }
        @Bean AuditEventRepository auditRepository() { return mock(AuditEventRepository.class); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean AuditTrailService audit(AuditEventRepository records, UserService users) { return new AuditTrailServiceImpl(records, users); }
        @Bean EmployeeExitService exits(EmployeeRepository employees, UserRepository users,
                RecruitmentDesignationVacancyRepository vacancies, AuditTrailService audit) {
            return new EmployeeExitServiceImpl(employees, users, vacancies, audit);
        }
    }
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private EmployeeExitService exits;

    @BeforeEach void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class);
        jdbc.execute("create table employee_state (id bigint primary key, status varchar, active boolean)");
        jdbc.execute("insert into employee_state values (5, 'ACTIVE', true)");
        jdbc.execute("create table exit_audit (ip varchar(64), action varchar(80), metadata varchar(10000))");
        var employee = new EmployeeEntity();
        employee.setEmployeeId(5L);
        employee.setStatus("ACTIVE");
        var account = new User();
        account.setId(50L);
        account.setActive(true);
        employee.setUser(account);
        var hr = new User();
        hr.setId(10L);
        hr.setEmail("hr@example.test");
        var employees = context.getBean(EmployeeRepository.class);
        var users = context.getBean(UserRepository.class);
        when(employees.lockForExit(5L)).thenReturn(Optional.of(employee));
        when(users.findByEmailIgnoreCaseAndActiveTrue("hr@example.test")).thenReturn(Optional.of(hr));
        when(employees.save(any())).thenAnswer(call -> {
            EmployeeEntity changed = call.getArgument(0);
            jdbc.update("update employee_state set status=? where id=5", changed.getStatus());
            return changed;
        });
        when(users.save(any())).thenAnswer(call -> {
            User changed = call.getArgument(0);
            jdbc.update("update employee_state set active=? where id=5", changed.getActive());
            return changed;
        });
        when(context.getBean(AuditEventRepository.class).save(any())).thenAnswer(call -> {
            AuditEventEntity entry = call.getArgument(0);
            jdbc.update("insert into exit_audit values (?, ?, ?)", entry.getIpAddress(), entry.getActionType(), entry.getMetadataJson());
            return entry;
        });
        exits = context.getBean(EmployeeExitService.class);
        login("ROLE_HR");
    }
    private void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "hr@example.test", null, List.of(new SimpleGrantedAuthority(role))));
    }
    @AfterEach void close() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void storesExitWithActorIpAndAuditInSameTransaction() {
        try (var ignored = AuditRequestContext.open("2001:db8::21")) {
            exits.exitEmployee(5L, LocalDate.of(2026, 1, 1), "Employee left", "hr@example.test");
        }
        assertThat(jdbc.queryForObject("select active from employee_state where id=5", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select status from employee_state where id=5", String.class)).isEqualTo("RESIGNED");
        assertThat(jdbc.queryForObject("select ip from exit_audit", String.class)).contains("2001:db8");
        assertThat(jdbc.queryForObject("select metadata from exit_audit", String.class)).contains("Employee left", "clientIp");
    }
    @Test void auditFailureRollsBackBothEmployeeStatusAndAccountAccess() {
        doThrow(new IllegalStateException("Audit unavailable")).when(context.getBean(AuditEventRepository.class)).save(any());
        assertThatThrownBy(() -> exits.exitEmployee(5L, LocalDate.of(2026, 1, 1), "Employee left", "hr@example.test"))
                .hasMessageContaining("Audit unavailable");
        assertThat(jdbc.queryForObject("select status from employee_state where id=5", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select active from employee_state where id=5", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from exit_audit", Integer.class)).isZero();
    }
    @Test void serviceAlsoRejectsNonHrCallers() {
        login("ROLE_EMPLOYEE");
        assertThatThrownBy(() -> exits.exitEmployee(5L, LocalDate.of(2026, 1, 1), "Employee left", "hr@example.test"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(jdbc.queryForObject("select active from employee_state where id=5", Boolean.class)).isTrue();
    }
}