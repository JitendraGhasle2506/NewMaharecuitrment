package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeHierarchyService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

class EmployeeHierarchyControllerTest {
    private AnnotationConfigApplicationContext context;
    private EmployeeHierarchyController controller;

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean EmployeeHierarchyService hierarchy() { return mock(EmployeeHierarchyService.class); }
        @Bean EmployeeHierarchyRepository repository() { return mock(EmployeeHierarchyRepository.class); }
        @Bean FileStorageService storage() { return mock(FileStorageService.class); }
        @Bean EmployeeHierarchyController controller(EmployeeHierarchyService hierarchy,
                EmployeeHierarchyRepository repository, FileStorageService storage) {
            return new EmployeeHierarchyController(hierarchy, repository, storage);
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(EmployeeHierarchyController.class);
        login("ROLE_HR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void returnsExpectedJsonWithPrimaryDefaultAndThymeleafView() throws Exception {
        EmployeeHierarchyNode node = new EmployeeHierarchyNode(1001L, "HOD Name", "EMP001", "HOD", "Finance", null, 0, true);
        when(context.getBean(EmployeeHierarchyService.class).tree(1001L, null, EmployeeReportingType.PRIMARY,
                null, null, 0, 25, 1)).thenReturn(node);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/api/employees/hierarchy/1001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(1001))
                .andExpect(jsonPath("$.data.children").isEmpty())
                .andExpect(jsonPath("$.data.hasChildren").value(false));
        assertThat(controller.page(new ExtendedModelMap())).isEqualTo("hr/employee-hierarchy");
        mvc.perform(get("/api/employees/hierarchy/1001").param("reportingType", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniesNonHrAccessToPageDataSearchAndPhotos() {
        login("ROLE_EMPLOYEE");
        assertThatThrownBy(() -> controller.page(new ExtendedModelMap())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(controller::options).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.tree(1L, EmployeeReportingType.PRIMARY, null, null, null, 0, 25, 1))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.search(1L, EmployeeReportingType.PRIMARY, null, null, "EMP"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.photo(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingOrUnmanagedPhotoReturnsNotFound() {
        assertThat(controller.photo(1L).getStatusCode().value()).isEqualTo(404);
        var source = mock(EmployeeHierarchyRepository.PhotoRow.class);
        when(source.getEmployeePhoto()).thenReturn("/outside-storage/private-file.jpg");
        when(context.getBean(EmployeeHierarchyRepository.class).findPhotoSources(1L)).thenReturn(List.of(source));
        assertThat(controller.photo(1L).getStatusCode().value()).isEqualTo(404);
    }

    private void login(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", "unused", List.of(new SimpleGrantedAuthority(authority))));
    }
}
