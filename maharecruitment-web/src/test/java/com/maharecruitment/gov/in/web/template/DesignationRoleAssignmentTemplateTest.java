package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.maharecruitment.gov.in.recruitment.service.model.DesignationEmployeeRoleView;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentView;

class DesignationRoleAssignmentTemplateTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/templates/admin/designation-role-assignments/list.html");

    @Test
    void pageRendersDesignationRoleAndEmployeeAssignmentStatus() throws Exception {
        String template = Files.readString(TEMPLATE_PATH);
        int pageStart = template.indexOf("<section class=\"admin-management-page\">");
        int pageEnd = template.lastIndexOf("</section>") + "</section>".length();
        String page = template.substring(pageStart, pageEnd)
                .replaceAll("\\s+th:(?:href|action)=\"[^\"]*\"", "");

        DesignationEmployeeRoleView employee = new DesignationEmployeeRoleView(
                100L,
                "EMP100",
                "Sanjay Patil",
                7L,
                true,
                List.of("ROLE_EMPLOYEE"),
                false);
        DesignationRoleAssignmentView assignment = new DesignationRoleAssignmentView(
                10L,
                "Senior Technical Manager",
                "Management",
                "ROLE_STM",
                true,
                1,
                1,
                0,
                1,
                List.of(employee));

        Context context = new Context();
        context.setVariable("assignments", List.of(assignment));
        context.setVariable("designationOptions", List.of(assignment));
        context.setVariable("availableRoleNames", List.of("ROLE_EMPLOYEE", "ROLE_STM"));
        context.setVariable("searchTerm", "");
        context.setVariable("totalEmployees", 1L);
        context.setVariable("pendingUsers", 1L);
        context.setVariable("unconfiguredDesignations", 0L);
        context.setVariable("_csrf", Map.of("parameterName", "_csrf", "token", "test-token"));

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        String rendered = engine.process(page, context);

        assertThat(rendered)
                .contains("Designation role assignment")
                .contains("Assign role by designation")
                .contains("designationAssignmentSelect")
                .contains("designationRoleSelect")
                .contains("Senior Technical Manager")
                .contains("ROLE_STM")
                .contains("Sanjay Patil")
                .contains("EMP100")
                .contains("Pending")
                .contains("Save &amp; assign");
    }
}
