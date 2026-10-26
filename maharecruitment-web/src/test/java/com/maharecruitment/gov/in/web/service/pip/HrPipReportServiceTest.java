package com.maharecruitment.gov.in.web.service.pip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.*;
import com.maharecruitment.gov.in.recruitment.repository.*;
import com.maharecruitment.gov.in.web.service.pip.impl.HrPipReportServiceImpl;

class HrPipReportServiceTest {
    @Configuration(proxyBeanMethods=false) @EnableMethodSecurity
    static class Config {
        @Bean UserRepository users(){return mock(UserRepository.class);}
        @Bean EmployeePipReportRepository reports(){return mock(EmployeePipReportRepository.class);}
        @Bean EmployeePipPlanRepository plans(){return mock(EmployeePipPlanRepository.class);}
        @Bean EmployeePipReviewRepository reviews(){return mock(EmployeePipReviewRepository.class);}
        @Bean HrPipReportService service(UserRepository users,EmployeePipReportRepository reports,EmployeePipPlanRepository plans,EmployeePipReviewRepository reviews){
            return new HrPipReportServiceImpl(users,reports,plans,reviews);
        }
    }
    private AnnotationConfigApplicationContext context;
    private HrPipReportService service;
    private EmployeePipReportRepository reports;
    private EmployeePipPlanRepository plans;
    @BeforeEach void setup(){
        context=new AnnotationConfigApplicationContext(Config.class);service=context.getBean(HrPipReportService.class);
        reports=context.getBean(EmployeePipReportRepository.class);plans=context.getBean(EmployeePipPlanRepository.class);
        when(context.getBean(UserRepository.class).existsByEmailIgnoreCaseAndActiveTrue("hr@example.test")).thenReturn(true);
        when(reports.findOverview(any(),any(),any())).thenReturn(Page.empty());login("ROLE_HR");
    }
    private void login(String role){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("hr@example.test",null,List.of(new SimpleGrantedAuthority(role))));}
    @AfterEach void close(){SecurityContextHolder.clearContext();context.close();}
    @Test void hrNeedsNoEmployeeProfileOrReportingScopeToReadAllStatuses(){
        var overview=service.overview("hr@example.test",null,"",0);
        assertThat(overview.plans()).isEmpty(); verify(reports).findOverview(isNull(),eq("%%"),eq(PageRequest.of(0,12)));
        verify(plans,never()).save(any());
    }
    @Test void managersEmployeesAndAdminsCannotUseTheHrOrganisationWideReport(){
        for(String role:List.of("ROLE_COO","ROLE_HOD","ROLE_MD","ROLE_CTO","ROLE_CFO","ROLE_EMPLOYEE","ROLE_ADMIN")){
            login(role);assertThatThrownBy(()->service.overview("hr@example.test",null,"",0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.detail("hr@example.test",100L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        verifyNoInteractions(reports);
    }
    @Test void validatesFiltersAndEscapesLiteralSearchCharacters(){
        service.overview("hr@example.test",EmployeePipPlan.Status.UNDER_REVIEW," A_100%! ",2);
        verify(reports).findOverview(eq(EmployeePipPlan.Status.UNDER_REVIEW),eq("%a!_100!%!!%"),eq(PageRequest.of(2,12)));
        assertThatThrownBy(()->service.overview("hr@example.test",null,"",-1)).hasMessageContaining("400");
        assertThatThrownBy(()->service.overview("hr@example.test",null,"x".repeat(101),0)).hasMessageContaining("400");
    }
    @Test void detailRetainsReasonFeedbackAndExitedEmployeeStatusWithoutWriting(){
        var row=mock(EmployeePipReportRepository.Row.class); when(row.getId()).thenReturn(100L);when(row.getEmployeeId()).thenReturn(50L);
        when(row.getName()).thenReturn("Outside reporting team");when(row.getEmployeeStatus()).thenReturn("RESIGNED");
        when(row.getStatus()).thenReturn(EmployeePipPlan.Status.EXIT_RECORDED);when(reports.findSummaryById(100L)).thenReturn(Optional.of(row));
        var plan=new EmployeePipPlan();plan.setReason("Missed deadlines");plan.setObjectives("Improve delivery");plan.setDecisionFeedback("No improvement");plan.setExitDate(LocalDate.of(2026,10,20));
        when(plans.findById(100L)).thenReturn(Optional.of(plan));
        var result=service.detail("hr@example.test",100L);
        assertThat(result.reason()).isEqualTo("Missed deadlines");assertThat(result.decisionFeedback()).isEqualTo("No improvement");assertThat(result.employeeActive()).isFalse();
        verify(plans,never()).save(any());verify(context.getBean(EmployeePipReviewRepository.class),never()).save(any());
    }
    @Test void disabledHrAndUnknownPlansAreRejected(){
        assertThatThrownBy(()->service.detail("hr@example.test",404L)).hasMessageContaining("404");
        when(context.getBean(UserRepository.class).existsByEmailIgnoreCaseAndActiveTrue("hr@example.test")).thenReturn(false);
        assertThatThrownBy(()->service.overview("hr@example.test",null,"",0)).hasMessageContaining("403");
    }
}