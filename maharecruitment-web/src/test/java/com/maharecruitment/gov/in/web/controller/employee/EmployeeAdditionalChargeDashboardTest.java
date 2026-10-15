package com.maharecruitment.gov.in.web.controller.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository.PersonalCharge;
import com.maharecruitment.gov.in.recruitment.service.AdditionalChargeService;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.service.employee.EmployeeDashboardContentService;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafView;
import org.thymeleaf.templateresolver.StringTemplateResolver;

class EmployeeAdditionalChargeDashboardTest {
    @Test void dashboardEmbedsTheSharedSelfHierarchyEvenWithoutAPersonalProfile() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/templates/employee/dashboard.html"))
                .replaceAll("th:replace=\"~\\{header/header[^\"]*\"", "");
        var request = new MockHttpServletRequest("GET", "/employee/dashboard");
        var application = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(new MockServletContext());
        var context = new org.thymeleaf.context.WebContext(application.buildExchange(request, new MockHttpServletResponse()));
        var profile = new EmployeeProfileDTO();
        profile.setEmployeeId(1L);
        profile.setProfileAvailable(true);
        profile.setFullName("Ananya Deshmukh");
        context.setVariable("profile", profile);
        context.setVariable("personalAdditionalCharges", List.of());
        context.setVariable("showTeamAttendance", true);
        context.setVariable("hierarchyApi", "/employee/api/employee-hierarchy");
        context.setVariable("hierarchyPhotoApi", "");
        context.setVariable("hierarchySelfMode", true);
        context.setVariable("dashboardContent", new com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView(
                LocalDate.of(2026, 10, 15), "Thursday", List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
        var engine = new SpringTemplateEngine();
        var fragments = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        fragments.setPrefix("templates/");
        fragments.setSuffix(".html");
        engine.addTemplateResolver(fragments);
        Files.createDirectories(Path.of("target/test-classes/templates"));
        Files.writeString(Path.of("target/test-classes/templates/dashboard-hierarchy-preview.html"), page);
        String html = engine.process("dashboard-hierarchy-preview", context);
        assertThat(html).contains("class=\"dashboard-team-hierarchy\"", "id=\"employeeHierarchy\"",
                "data-api=\"/employee/api/employee-hierarchy\"", "data-self-mode=\"true\"", "My team hierarchy",
                "/css/employee-hierarchy.css?v=20261016", "/js/employee-hierarchy.js?v=20261017")
                .doesNotContain("Manage reporting");
        assertThat(html.split("id=\"employeeHierarchy\"", -1)).hasSize(2);
        Files.writeString(Path.of("target/employee-dashboard-hierarchy-preview.html"), html);
        profile.setProfileAvailable(false);
        String withoutPersonalProfile = engine.process("dashboard-hierarchy-preview", context);
        assertThat(withoutPersonalProfile).contains("id=\"employeeHierarchy\"", "data-self-mode=\"true\"",
                "href=\"/employee/employee-hierarchy\"");
        // Browser fixture deliberately uses an employee without a separate personal-profile record.
        Files.writeString(Path.of("target/employee-dashboard-hierarchy-preview.html"), withoutPersonalProfile);
    }

    @Test void chargesAreLoadedForAuthenticatedLoginRatherThanProfileEmail() {
        var profiles = mock(EmployeeProfileService.class);
        var content = mock(EmployeeDashboardContentService.class);
        var charges = mock(AdditionalChargeService.class);
        var profile = new EmployeeProfileDTO();
        profile.setEmail("different-profile@example.test");
        when(profiles.getCurrentEmployeeProfile("signed-in@example.test")).thenReturn(profile);
        var ownCharges = List.of(mock(PersonalCharge.class));
        when(charges.personalActiveCharges("signed-in@example.test")).thenReturn(ownCharges);
        var model = new ExtendedModelMap();
        new EmployeeDashboardController(profiles, content, charges)
                .dashboard(() -> "signed-in@example.test", new MockHttpSession(), model);
        assertThat(model.get("personalAdditionalCharges")).isSameAs(ownCharges);
        assertThat(model).containsEntry("hierarchyApi", "/employee/api/employee-hierarchy")
                .containsEntry("hierarchySelfMode", true).containsEntry("hierarchyPhotoApi", "");
        verify(charges).personalActiveCharges("signed-in@example.test");
        verifyNoMoreInteractions(charges);
    }

    static class TemplateView extends ThymeleafView {
        TemplateView(SpringTemplateEngine engine) { setTemplateEngine(engine); setLocale(Locale.ENGLISH); }
    }

    @Test void noticeRendersScopeDatesEscapedDutiesAndLinksAndHidesWhenUnassigned() throws Exception {
        String page = Files.readString(Path.of("src/main/resources/templates/employee/dashboard.html"));
        String section = page.substring(page.indexOf("<section class=\"employee-charge-notice\""),
                page.indexOf("            <div class=\"profile-grid\">"));
        var servlet = new MockServletContext();
        try (var application = new StaticWebApplicationContext()) {
            application.setServletContext(servlet);
            application.refresh();
            servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, application);
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(new StringTemplateResolver());
            var view = new TemplateView(engine);
            view.setApplicationContext(application);
            view.setTemplateName(section);
            var request = new MockHttpServletRequest(servlet, "GET", "/portal/employee/dashboard");
            request.setContextPath("/portal");
            var charge = mock(PersonalCharge.class);
            when(charge.getPosition()).thenReturn("ROLE_HOD");
            when(charge.getReplacedName()).thenReturn("Rohan Patil");
            when(charge.getScope()).thenReturn("All cells");
            when(charge.getStartDate()).thenReturn(LocalDate.of(2026, 10, 15));
            when(charge.getResponsibilities()).thenReturn("<script>alert(1)</script>");
            var profile = new EmployeeProfileDTO();
            profile.setProfileAvailable(true);
            var response = new MockHttpServletResponse();
            view.render(Map.of("profile", profile, "personalAdditionalCharges", List.of(charge)), request, response);
            assertThat(response.getContentAsString()).contains("Your additional charges", "Rohan Patil", "All cells",
                    "2026-10-15", "Until further orders", "&lt;script&gt;alert(1)&lt;/script&gt;",
                    "href=\"/portal/employee/employee-hierarchy\"", "href=\"/portal/hod1/leaveApprovals\"")
                    .doesNotContain("<script>alert(1)</script>", "No replacement team or approval authority is transferred.");
            when(charge.getReplacedName()).thenReturn(null);
            response = new MockHttpServletResponse();
            view.render(Map.of("profile", profile, "personalAdditionalCharges", List.of(charge)), request, response);
            assertThat(response.getContentAsString()).contains("No replacement team or approval authority is transferred.");
            response = new MockHttpServletResponse();
            view.render(Map.of("profile", profile, "personalAdditionalCharges", List.of()), request, response);
            assertThat(response.getContentAsString()).doesNotContain("Your additional charges");
        }
    }
}
