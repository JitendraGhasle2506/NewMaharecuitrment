package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceManagementService;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceManagementService.EmployeeTypeFilter;

class ResourceManagementControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean ResourceManagementService service() { return mock(ResourceManagementService.class); }
        @Bean ResourceManagementController controller(ResourceManagementService service) {
            return new ResourceManagementController(service);
        }
    }

    private AnnotationConfigApplicationContext context;
    private ResourceManagementController controller;
    private ResourceManagementService service;
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(ResourceManagementController.class);
        service = context.getBean(ResourceManagementService.class);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(10L, "Manager", "manager@example.test",
                List.of("ROLE_EMPLOYEE"), null, null, null, LocalDateTime.now(), LocalDateTime.now()));
        login("ROLE_EMPLOYEE");
    }

    @AfterEach void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test void returnsPageAndUsesSessionIdentityForAllOperations() throws Exception {
        assertThat(controller.page(session)).isEqualTo("employee/resource-management");
        when(service.list(10L, "mapped", "", 0, 12, EmployeeTypeFilter.INTERNAL_MAHAIT)).thenReturn(
                new ResourceManagementService.Page(List.of(), List.of(), 0, 0, 12, 0, 0, 0));
        mvc.perform(get("/reporting-manager/resources/api").session(session).param("userId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        verify(service).list(10L, "mapped", "", 0, 12, EmployeeTypeFilter.INTERNAL_MAHAIT);
        mvc.perform(post("/reporting-manager/resources/api/4/map").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"managerEmployeeId\":2,\"userId\":999}"))
                .andExpect(status().isOk());
        verify(service).map(10L, 4L, 2L);
        mvc.perform(post("/reporting-manager/resources/api/3/reassign").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"managerEmployeeId\":2,\"userId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Reporting manager updated successfully."));
        verify(service).reassign(10L, 3L, 2L);
        mvc.perform(post("/reporting-manager/resources/api/3/release").session(session))
                .andExpect(status().isOk());
        verify(service).release(10L, 3L);
    }

    @Test void defaultsEmployeeTypeAndBindsExplicitFiltersAndRejectsUnknownTypes() throws Exception {
        mvc.perform(get("/reporting-manager/resources/api").session(session).param("view", "free"))
                .andExpect(status().isOk());
        verify(service).list(10L, "free", "", 0, 12, EmployeeTypeFilter.INTERNAL_MAHAIT);
        mvc.perform(get("/reporting-manager/resources/api").session(session)
                .param("view", "free").param("employeeType", "EXTERNAL"))
                .andExpect(status().isOk());
        verify(service).list(10L, "free", "", 0, 12, EmployeeTypeFilter.EXTERNAL);
        mvc.perform(get("/reporting-manager/resources/api").session(session).param("employeeType", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectsExpiredSessionAndInvalidAssignment() throws Exception {
        mvc.perform(get("/reporting-manager/resources/api")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        for (String body : List.of("{}", "{\"managerEmployeeId\":0}", "{\"managerEmployeeId\":-1}")) {
            mvc.perform(post("/reporting-manager/resources/api/4/map").session(session)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
            mvc.perform(post("/reporting-manager/resources/api/3/reassign").session(session)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void returnsActionableConflictAsJson() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already mapped"))
                .when(service).map(10L, 4L, 2L);
        mvc.perform(post("/reporting-manager/resources/api/4/map").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"managerEmployeeId\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Already mapped"));
    }

    @Test void allowsReportingRolesAndRejectsUnrelatedRoles() {
        for (String role : List.of("ROLE_EMPLOYEE", "ROLE_HOD", "ROLE_STM", "ROLE_PM", "ROLE_COO", "ROLE_CTO", "ROLE_CFO", "ROLE_MD")) {
            login(role);
            assertThat(controller.page(session)).isEqualTo("employee/resource-management");
        }
        for (String role : List.of("ROLE_AGENCY", "ROLE_DEPARTMENT", "ROLE_USER")) {
            login(role);
            assertThatThrownBy(() -> controller.page(session)).isInstanceOf(AccessDeniedException.class);
        }
    }

    private static void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "manager", "n/a", List.of(new SimpleGrantedAuthority(role))));
    }
}
