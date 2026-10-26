package com.maharecruitment.gov.in.web.service.pip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.*;
import com.maharecruitment.gov.in.recruitment.repository.*;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository.*;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceReportingScopeResolver;
import com.maharecruitment.gov.in.web.service.hr.EmployeeOffboardingService;
import com.maharecruitment.gov.in.web.service.pip.impl.EmployeePipServiceImpl;

class EmployeePipServiceTest {
    @Configuration(proxyBeanMethods=false) @EnableMethodSecurity @org.springframework.transaction.annotation.EnableTransactionManagement
    static class Config {
        @Bean javax.sql.DataSource dataSource() { return new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:pip_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""); }
        @Bean org.springframework.jdbc.datasource.DataSourceTransactionManager transactionManager(javax.sql.DataSource data) { return new org.springframework.jdbc.datasource.DataSourceTransactionManager(data); }
        @Bean org.springframework.jdbc.core.JdbcTemplate jdbc(javax.sql.DataSource data) { return new org.springframework.jdbc.core.JdbcTemplate(data); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean EmployeeRepository employees() { return mock(EmployeeRepository.class); }
        @Bean EmployeeHierarchyRepository hierarchy() { return mock(EmployeeHierarchyRepository.class); }
        @Bean EmployeePipPlanRepository plans() { return mock(EmployeePipPlanRepository.class); }
        @Bean EmployeePipReviewRepository reviews() { return mock(EmployeePipReviewRepository.class); }
        @Bean AuditTrailService audit() { return mock(AuditTrailService.class); }
        @Bean EmployeeOffboardingService offboarding() { return mock(EmployeeOffboardingService.class); }
        @Bean ResourceReportingScopeResolver scopes(EmployeeHierarchyRepository hierarchy) { return new ResourceReportingScopeResolver(hierarchy); }
        @Bean EmployeePipService service(UserRepository users, EmployeeRepository employees, ResourceReportingScopeResolver scopes,
                EmployeePipPlanRepository plans, EmployeePipReviewRepository reviews, AuditTrailService audit, EmployeeOffboardingService offboarding) {
            return new EmployeePipServiceImpl(users,employees,scopes,plans,reviews,audit,offboarding);
        }
    }
    private AnnotationConfigApplicationContext context;
    private EmployeePipService service;
    private EmployeePipPlanRepository plans;
    private EmployeePipReviewRepository reviews;
    private EmployeeRepository employees;
    private EmployeeHierarchyRepository hierarchy;
    private AuditTrailService audit;
    private EmployeePipPlan plan;
    private final LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @BeforeEach void setup() {
        context=new AnnotationConfigApplicationContext(Config.class);
        service=context.getBean(EmployeePipService.class); plans=context.getBean(EmployeePipPlanRepository.class);
        reviews=context.getBean(EmployeePipReviewRepository.class); employees=context.getBean(EmployeeRepository.class);
        hierarchy=context.getBean(EmployeeHierarchyRepository.class); audit=context.getBean(AuditTrailService.class);
        User actor=new User(); actor.setId(10L); actor.setName("Manager"); actor.setEmail("manager@example.test");
        when(context.getBean(UserRepository.class).findByEmailIgnoreCaseAndActiveTrue(actor.getEmail())).thenReturn(Optional.of(actor));
        var people=List.of(person(1,10),person(2,20),person(3,30),person(4,40));
        when(hierarchy.findEmployeesIncludingInactive()).thenReturn(people);
        var reporting=List.of(link(2,1),link(3,2));
        when(hierarchy.findReportingRelationships("PRIMARY")).thenReturn(reporting);
        for(long id: new long[]{1,2,3,4}) {
            var employee=new EmployeeEntity(); employee.setEmployeeId(id); employee.setStatus("ACTIVE");
            when(employees.lockForExit(id)).thenReturn(Optional.of(employee));
        }
        plan=new EmployeePipPlan(); plan.setId(100L); plan.setEmployeeId(2L); plan.setStartDate(today.minusDays(10));
        plan.setEndDate(today.plusDays(20)); plan.setDurationMonths(1); plan.setObjectives("Improve delivery");
        when(plans.findById(100L)).thenAnswer(call -> Optional.of(plan));
        when(plans.lockById(100L)).thenAnswer(call -> Optional.of(plan));
        when(plans.save(any())).thenAnswer(call -> {plan=call.getArgument(0); if(plan.getId()==null)plan.setId(100L); return plan;});
        when(reviews.save(any())).thenAnswer(call -> {EmployeePipReview review=call.getArgument(0); review.setId(200L); return review;});
        when(plans.findByEmployeeIdInOrderByCreatedDateTimeDescIdDesc(anyCollection(),any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plan)));
        login("ROLE_COO");
    }
    private EmployeeRow person(long id,long user) {
        var p=mock(EmployeeRow.class); when(p.getEmployeeId()).thenReturn(id); when(p.getUserId()).thenReturn(user);
        when(p.getEmployeeName()).thenReturn("Employee "+id); when(p.getEmployeeCode()).thenReturn("EMP"+id);
        when(p.getEmployeeStatus()).thenReturn("ACTIVE"); when(p.getUserActive()).thenReturn(true); return p;
    }
    private ReportingRow link(long employee,long manager) {
        var row=mock(ReportingRow.class); when(row.getEmployeeId()).thenReturn(employee);
        when(row.getManagerEmployeeId()).thenReturn(manager); return row;
    }
    private void login(String role) { SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("manager@example.test",null,List.of(new SimpleGrantedAuthority(role)))); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void allFiveAllowedRolesCanStartOneOrTwoMonthPlansForNestedSubordinates() {
        for(String role:List.of("ROLE_COO","ROLE_HOD","ROLE_MD","ROLE_CTO","ROLE_CFO")) {
            login(role);
            for(int months:new int[]{1,2}) {
                assertThat(service.start("manager@example.test",3L,today,months,"Improve quality","  Repeated missed deadlines  ")).isEqualTo(100L);
                assertThat(plan.getEndDate()).isEqualTo(today.plusMonths(months).minusDays(1));
                assertThat(plan.getStatus()).isEqualTo(EmployeePipPlan.Status.UNDER_REVIEW);
                assertThat(plan.getReason()).isEqualTo("Repeated missed deadlines");
            }
        }
        verifyNoInteractions(context.getBean(EmployeeOffboardingService.class));
    }
    @Test void otherRolesCannotReadStartReviewOrDecidePip() {
        for(String role:List.of("ROLE_HR","ROLE_EMPLOYEE","ROLE_ADMIN","ROLE_STM","ROLE_PM","ROLE_HOD1")) {
            login(role);
            assertThatThrownBy(()->service.board("manager@example.test","",0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.start("manager@example.test",2L,today,1,"Goal","  Repeated missed deadlines  ")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.review("manager@example.test",100L,today,4,"Good progress")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.decide("manager@example.test",100L,EmployeePipService.Decision.CONTINUE,"Good",null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        verifyNoInteractions(audit);
    }
    @Test void excludesSelfAndEmployeesOutsideTheCurrentReportingTeam() {
        for(long id:new long[]{1,4}) {
            assertThatThrownBy(()->service.start("manager@example.test",id,today,1,"Goals","  Repeated missed deadlines  "))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("403");
            plan.setEmployeeId(id);
            assertThatThrownBy(()->service.detail("manager@example.test",100L)).hasMessageContaining("403");
        }
        verifyNoInteractions(audit);
    }
    @Test void rechecksScopeAfterReportingAssignmentChanges() {
        service.detail("manager@example.test",100L);
        when(hierarchy.findReportingRelationships("PRIMARY")).thenReturn(List.of());
        assertThatThrownBy(()->service.review("manager@example.test",100L,today,3,"Feedback")).hasMessageContaining("403");
        verify(reviews,never()).save(any());
    }
    @Test void appendsFeedbackAndAuditsEachReview() {
        service.review("manager@example.test",100L,today,3,"Improving delivery");
        service.review("manager@example.test",100L,today,4,"Quality improved");
        verify(reviews,times(2)).save(any());
        var capture=org.mockito.ArgumentCaptor.forClass(AuditRecordRequest.class); verify(audit,times(2)).record(capture.capture());
        assertThat(capture.getValue().getActionType()).isEqualTo("PIP_REVIEW_ADDED");
        assertThat(capture.getValue().getMetadata()).containsEntry("feedback","Quality improved");
    }
    @Test void satisfactoryDecisionClosesPipWithoutDisablingEmployment() {
        service.decide("manager@example.test",100L,EmployeePipService.Decision.CONTINUE,"Meets expectations",null);
        assertThat(plan.getStatus()).isEqualTo(EmployeePipPlan.Status.COMPLETED);
        verifyNoInteractions(context.getBean(EmployeeOffboardingService.class));
        verify(employees,never()).save(any());
        assertThatThrownBy(()->service.review("manager@example.test",100L,today,4,"Late edit")).hasMessageContaining("already been closed");
    }
    @Test void exitDecisionDelegatesToSharedAtomicOffboardingAndRetainsFeedback() {
        service.decide("manager@example.test",100L,EmployeePipService.Decision.EXIT,"No improvement",today);
        verify(context.getBean(EmployeeOffboardingService.class)).exitEmployee(2L,today,"No improvement","manager@example.test",EmployeeOffboardingService.Source.PIP);
        assertThat(plan.getStatus()).isEqualTo(EmployeePipPlan.Status.EXIT_RECORDED);
        assertThat(plan.getExitDate()).isEqualTo(today);
        assertThat(plan.getDecisionFeedback()).isEqualTo("No improvement");
    }
    @Test void rejectsDuplicatePlansAndInvalidDurationsDatesRatingsAndFeedback() {
        for(int months:new int[]{0,3}) assertThatThrownBy(()->service.start("manager@example.test",2L,today,months,"Goals","  Repeated missed deadlines  ")).hasMessageContaining("one or two");
        assertThatThrownBy(()->service.start("manager@example.test",2L,today.plusDays(1),1,"Goals","  Repeated missed deadlines  ")).hasMessageContaining("start date");
        when(plans.existsByEmployeeIdAndStatus(2L,EmployeePipPlan.Status.UNDER_REVIEW)).thenReturn(true);
        assertThatThrownBy(()->service.start("manager@example.test",2L,today,1,"Goals","  Repeated missed deadlines  ")).hasMessageContaining("already has");
        assertThatThrownBy(()->service.review("manager@example.test",100L,today,6,"Feedback")).hasMessageContaining("rating");
        assertThatThrownBy(()->service.review("manager@example.test",100L,today.minusDays(11),3,"Feedback")).hasMessageContaining("review date");
        assertThatThrownBy(()->service.review("manager@example.test",100L,today,3," ")).hasMessageContaining("required");
        assertThatThrownBy(()->service.decide("manager@example.test",100L,EmployeePipService.Decision.EXIT,"Reason",today.plusDays(1))).hasMessageContaining("Exit date");
        verifyNoInteractions(audit);
    }
    @Test void boardShowsOnlySubordinatesAndExcludesEmployeesAlreadyUnderReviewFromNewPlans() {
        when(plans.findEmployeeIdsByStatus(anyCollection(),eq(EmployeePipPlan.Status.UNDER_REVIEW))).thenReturn(List.of(2L));
        assertThat(service.board("manager@example.test","",0).employees()).extracting(EmployeePipService.EmployeeOption::id).containsExactly(3L);
    }
    @Test void auditFailureRollsBackPipOutcomeAndAccountDeactivationTogether() {
        var jdbc=context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        jdbc.execute("create table pip_atomic (plan_status varchar, employee_status varchar, active boolean)");
        jdbc.execute("insert into pip_atomic values ('UNDER_REVIEW', 'ACTIVE', true)");
        doAnswer(call->{
            EmployeePipPlan changed=call.getArgument(0);
            jdbc.update("update pip_atomic set plan_status=?",changed.getStatus().name()); return changed;
        }).when(plans).save(any());
        doAnswer(call->{jdbc.update("update pip_atomic set employee_status='RESIGNED', active=false");return null;})
                .when(context.getBean(EmployeeOffboardingService.class)).exitEmployee(any(),any(),any(),any(),any());
        doThrow(new IllegalStateException("Audit unavailable")).when(audit).record(any());
        assertThatThrownBy(()->service.decide("manager@example.test",100L,EmployeePipService.Decision.EXIT,"No improvement",today))
                .hasMessageContaining("Audit unavailable");
        assertThat(jdbc.queryForObject("select plan_status from pip_atomic",String.class)).isEqualTo("UNDER_REVIEW");
        assertThat(jdbc.queryForObject("select employee_status from pip_atomic",String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select active from pip_atomic",Boolean.class)).isTrue();
    }    @Test void pipReasonIsRequiredLengthLimitedAndIncludedInAuditAndDetail() {
        for (String invalid : new String[]{null,"   ","x".repeat(2001)}) {
            assertThatThrownBy(()->service.start("manager@example.test",2L,today,1,"Improve delivery",invalid))
                    .hasMessageContaining("Reason for PIP");
        }
        verify(plans,never()).save(any());
        service.start("manager@example.test",2L,today,1,"Improve delivery","  Repeated missed deadlines  ");
        assertThat(service.detail("manager@example.test",100L).reason()).isEqualTo("Repeated missed deadlines");
        var capture=org.mockito.ArgumentCaptor.forClass(AuditRecordRequest.class);
        verify(audit).record(capture.capture());
        assertThat(capture.getValue().getMetadata()).containsEntry("reason","Repeated missed deadlines");
    }}