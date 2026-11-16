package com.maharecruitment.gov.in.web.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService;
import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService.ChangeResult;
import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService.RoleOption;

class AdminInternalApproverRolePageControllerTest {

    private static final String TEMPLATE = "admin/internal-approver-roles/list";

    private InternalApproverRoleService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        service = mock(InternalApproverRoleService.class);
        String page = Files.readString(Path.of("src/main/resources/templates/" + TEMPLATE + ".html"))
                .replaceAll("th:replace=\"[^\"]*\"", "");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver() {
            @Override
            protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String owner,
                    String template, Map<String, Object> attributes) {
                return new StringTemplateResource(page);
            }
        });
        var resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(engine);
        resolver.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new AdminInternalApproverRolePageController(service))
                .setViewResolvers(resolver)
                .build();
    }

    @Test
    void screenShowsRoleChoicesWithoutTheChangeLogOrInternalDecisions() throws Exception {
        when(service.getRoleOptions()).thenReturn(List.of(
                new RoleOption(15L, "ROLE_EMPLOYEE", "EMPLOYEE", false),
                new RoleOption(17L, "ROLE_HOD", "HOD", true),
                new RoleOption(10L, "ROLE_PM", "PM", true)));

        String html = render(signedIn("admin@example.test", "ROLE_ADMIN"));

        assertThat(html)
                .contains("Internal approver roles", "INTERNAL employees only", "HOD or PM")
                .contains("name=\"roleIds\"", "value=\"17\"", "Save approver roles", "name=\"remarks\"")
                .contains("/admin/users", "/admin/roles", "/admin/role-menu-mappings")
                .doesNotContain("Change log", "Internal employee decisions", "change-log", "decision-log",
                        "changePage", "decisionPage");
        assertThat(html.split("checked=\"checked\"", -1)).hasSize(3);
        verify(service, never()).getChangeLog(anyInt(), anyInt());
        verify(service, never()).getInternalDecisionLog(anyInt(), anyInt());
        Files.writeString(Path.of("target/internal-approver-roles-preview.html"), html);
    }

    @Test
    void hrSeesTheRoleFormButNotTheAdminOnlyQuickLinks() throws Exception {
        when(service.getRoleOptions()).thenReturn(List.of(new RoleOption(17L, "ROLE_HOD", "HOD", true)));

        String html = render(signedIn("hr@example.test", "ROLE_HR"));

        assertThat(html)
                .contains("Internal approver roles", "name=\"roleIds\"", "Save approver roles")
                .doesNotContain("/admin/users", "/admin/roles", "/admin/role-menu-mappings", "Quick links");
    }

    private String render(UsernamePasswordAuthenticationToken user) throws Exception {
        return mvc.perform(get("/admin/internal-approver-roles")
                        .principal(user)
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static UsernamePasswordAuthenticationToken signedIn(String login, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(login, null, List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void savePassesTheSelectionReasonAndSignedInAdminAndReportsTheChange() throws Exception {
        when(service.updateApproverRoles(List.of(17L, 10L), "PM added", "admin@example.test"))
                .thenReturn(new ChangeResult(List.of("ROLE_PM"), List.of("ROLE_MD")));

        mvc.perform(post("/admin/internal-approver-roles")
                        .param("roleIds", "17", "10")
                        .param("remarks", "PM added")
                        .principal(() -> "admin@example.test"))
                .andExpect(redirectedUrl("/admin/internal-approver-roles"))
                .andExpect(flash().attribute("successMessage",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Added: ROLE_PM."),
                                org.hamcrest.Matchers.containsString("Removed: ROLE_MD."))));

        verify(service).updateApproverRoles(List.of(17L, 10L), "PM added", "admin@example.test");
    }

    @Test
    void rejectedSaveShowsTheReasonAndKeepsTheTypedRemarks() throws Exception {
        when(service.updateApproverRoles(null, "clear all", "admin@example.test"))
                .thenThrow(new IllegalArgumentException("Select at least one role."));

        mvc.perform(post("/admin/internal-approver-roles")
                        .param("remarks", "clear all")
                        .principal(() -> "admin@example.test"))
                .andExpect(redirectedUrl("/admin/internal-approver-roles"))
                .andExpect(flash().attribute("errorMessage", "Select at least one role."))
                .andExpect(flash().attribute("remarks", "clear all"));
    }
}
