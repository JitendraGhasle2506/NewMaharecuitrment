package com.maharecruitment.gov.in.web.controller.hr;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;

class EmployeeExitControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean EmployeeExitService exits() { return mock(EmployeeExitService.class); }
        @Bean HROnboardingPageService onboarding() { return mock(HROnboardingPageService.class); }
        @Bean EmployeeExitController controller(EmployeeExitService exits, HROnboardingPageService onboarding) {
            return new EmployeeExitController(exits, onboarding);
        }
    }
    private AnnotationConfigApplicationContext context;
    private MockMvc mvc;
    private EmployeeExitService exits;
    private final String date = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();

    @BeforeEach void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        exits = context.getBean(EmployeeExitService.class);
        var onboarding = context.getBean(HROnboardingPageService.class);
        when(onboarding.loadEmployeeDetail(5L)).thenReturn(new EmployeeOnboardingDetailView(
                5L, "EMP005", "ACTIVE", "MAHAIT", null, new AgencyPreOnboardingForm()));
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(EmployeeExitController.class)).build();
        login("ROLE_HR");
    }
    private void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "hr@example.test", null, List.of(new SimpleGrantedAuthority(role))));
    }
    @AfterEach void close() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void hrCanOpenIndependentExitForm() throws Exception {
        mvc.perform(get("/hr/employees/5/exit")).andExpect(status().isOk())
                .andExpect(view().name("hr/employee-exit")).andExpect(model().attributeExists("exitForm", "employeeDetail", "today"));
    }
    @Test void confirmedExitDisablesEmployeeAndRedirectsToResignedList() throws Exception {
        mvc.perform(post("/hr/employees/5/exit").principal(() -> "hr@example.test")
                .param("lastWorkingDate", date).param("reason", "Employee left").param("confirmed", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/hr/employees/resigned"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(exits).exitEmployee(5L, LocalDate.parse(date), "Employee left", "hr@example.test");
    }
    @Test void missingConfirmationAndReasonCannotDeactivate() throws Exception {
        mvc.perform(post("/hr/employees/5/exit").principal(() -> "hr@example.test").param("lastWorkingDate", date))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("exitForm", "reason", "confirmed"));
        verifyNoInteractions(exits);
    }
    @Test void employeeRoleCannotSubmitOrOpenExitScreen() {
        login("ROLE_EMPLOYEE");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(get("/hr/employees/5/exit")))
                .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(post("/hr/employees/5/exit")
                .principal(() -> "hr@example.test").param("lastWorkingDate", date)
                .param("reason", "Employee left").param("confirmed", "true")))
                .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(exits);
    }
}