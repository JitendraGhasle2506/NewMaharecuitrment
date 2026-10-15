package com.maharecruitment.gov.in.web.controller.hr;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import com.maharecruitment.gov.in.master.repository.*;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AdditionalChargeControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean AdditionalChargeService service() { return mock(AdditionalChargeService.class); }
        @Bean AdditionalChargeRepository repository() { return mock(AdditionalChargeRepository.class); }
        @Bean CellMasterRepository cells() { return mock(CellMasterRepository.class); }
        @Bean AdditionalChargeController controller(AdditionalChargeService service, AdditionalChargeRepository repository,
                CellMasterRepository cells) {
            return new AdditionalChargeController(service, repository, cells);
        }
    }
    AnnotationConfigApplicationContext context;
    AdditionalChargeController controller;
    AdditionalChargeService service;
    MockMvc mvc;

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(AdditionalChargeController.class);
        service = context.getBean(AdditionalChargeService.class);
        when(service.list(0)).thenReturn(Page.empty());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "hr@example.test", "", List.of(new SimpleGrantedAuthority("ROLE_HR"))));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void validFormBindsDatesAndSavesTheAssignment() throws Exception {
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr@example.test")
                .param("assignee", "E:1").param("position", "HOD").param("scope", "C:2")
                .param("startDate", "2026-10-15").param("endDate", "2026-11-15"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/hr/additional-charge"))
                .andExpect(flash().attributeExists("successMessage"));
        var captor = ArgumentCaptor.forClass(AdditionalChargeService.Request.class);
        verify(service).assign(captor.capture(), eq("hr@example.test"));
        assertThat(captor.getValue().startDate()).hasToString("2026-10-15");
        assertThat(captor.getValue().endDate()).hasToString("2026-11-15");
    }

    @Test void invalidFormRetainsFieldsAndDoesNotSave() throws Exception {
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr")
                .param("assignee", "E:1").param("position", "HOD").param("scope", "C:2"))
                .andExpect(status().isOk()).andExpect(view().name("hr/additional-charge"))
                .andExpect(model().attributeHasFieldErrors("chargeForm", "startDate"));
        verify(service, never()).assign(any(), any());
    }

    @Test void overlappingChargeReturnsReadableErrorAndKeepsForm() throws Exception {
        doThrow(new IllegalArgumentException("Overlapping charge.")).when(service).assign(any(), any());
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr")
                .param("assignee", "E:1").param("position", "HOD").param("scope", "C:2")
                .param("startDate", "2026-10-15"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("chargeForm"));
    }

    @Test void revokeIsPostedAndAudited() throws Exception {
        mvc.perform(post("/hr/additional-charge/4/revoke").principal(() -> "hr@example.test"))
                .andExpect(status().is3xxRedirection());
        verify(service).revoke(4L, "hr@example.test");
    }

    @Test void replacementIsBoundAndDepartmentScopesAreRejected() throws Exception {
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr")
                .param("assignee", "E:1").param("position", "ROLE_HOD").param("scope", "C:2")
                .param("replaces", "E:3").param("startDate", "2026-10-15"))
                .andExpect(status().is3xxRedirection());
        var captor = ArgumentCaptor.forClass(AdditionalChargeService.Request.class);
        verify(service).assign(captor.capture(), eq("hr"));
        assertThat(captor.getValue().replaces()).isEqualTo("E:3");
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr")
                .param("assignee", "E:1").param("position", "ROLE_HOD").param("scope", "D:2")
                .param("startDate", "2026-10-15"))
                .andExpect(model().attributeHasFieldErrors("chargeForm", "scope"));
        verify(service, times(1)).assign(any(), any());
    }

    @Test void previewShowsTheSelectedReplacementTeam() throws Exception {
        var person = mock(AdditionalChargeRepository.Choice.class);
        when(person.getId()).thenReturn(5L);
        when(person.getLabel()).thenReturn("Member Five (EMP005)");
        when(service.previewTeam("C:2", "E:3")).thenReturn(List.of(person));
        mvc.perform(get("/hr/additional-charge/team-preview").param("scope", "C:2").param("replaces", "E:3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].label").value("Member Five (EMP005)"));
    }

    @Test void missingCellIsAcceptedAndDefaultsToGlobalCoverage() throws Exception {
        mvc.perform(post("/hr/additional-charge").principal(() -> "hr")
                .param("assignee", "E:1").param("position", "ROLE_HOD").param("replaces", "E:3")
                .param("startDate", "2026-10-15"))
                .andExpect(status().is3xxRedirection());
        var captor = ArgumentCaptor.forClass(AdditionalChargeService.Request.class);
        verify(service).assign(captor.capture(), eq("hr"));
        assertThat(captor.getValue().scope()).isNull();
        mvc.perform(get("/hr/additional-charge/team-preview").param("replaces", "E:3"))
                .andExpect(status().isOk());
        verify(service).previewTeam("", "E:3");
    }

    @Test void nonHrCannotReadCreateOrRevokeCharges() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "employee", "", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
        assertThatThrownBy(() -> controller.page(0, new ExtendedModelMap())).isInstanceOf(AccessDeniedException.class);
        var request = new AdditionalChargeService.Request("E:1", "HOD", "D:2", java.time.LocalDate.now(), null, "", "");
        assertThatThrownBy(() -> controller.assign(request, new BeanPropertyBindingResult(request, "chargeForm"),
                () -> "employee", new ExtendedModelMap(), new RedirectAttributesModelMap())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.revoke(1L, () -> "employee", new RedirectAttributesModelMap()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.preview("C:2", "E:3")).isInstanceOf(AccessDeniedException.class);
    }
}
