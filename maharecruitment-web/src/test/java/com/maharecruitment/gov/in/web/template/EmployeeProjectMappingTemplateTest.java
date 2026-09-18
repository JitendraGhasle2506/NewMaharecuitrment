package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EmployeeProjectMappingTemplateTest {

    private static final Path TEMPLATE_ROOT = Path.of("src/main/resources/templates/hr");

    @Test
    void listProvidesDirectEmployeeProjectManagementWithoutEmployeeCode() throws IOException {
        String html = Files.readString(TEMPLATE_ROOT.resolve("employee-project-mapping-list.html"));
        assertThat(html)
                .contains("Employee Project Mapping")
                .contains("Assign Employees")
                .contains("Mapped Employees")
                .contains("Eligible Project Scope")
                .contains("/hr/employee-project-mappings/{employeeId}")
                .contains("employeeProjectBulkMappingForm")
                .contains("/hr/employee-project-mappings/bulk")
                .contains("employee-project-row-check")
                .contains("data-scope=${project.scope}")
                .contains("Assign Selected")
                .doesNotContain("employee.employeeCode");
    }

    @Test
    void mappedViewShowsEmployeeProjectSearchAndPagination() throws IOException {
        String html = Files.readString(TEMPLATE_ROOT.resolve("employee-project-mapped-list.html"));
        assertThat(html)
                .contains("Mapped Employee Projects")
                .contains("Employee, email, project name or code")
                .contains("Mapped Project")
                .contains("Project Scope")
                .contains("source='mapped'")
                .contains("employeePage.totalPages > 1")
                .doesNotContain("employee.employeeCode");
    }

    @Test
    void formExplainsAndEnforcesScopeSpecificChoices() throws IOException {
        String html = Files.readString(TEMPLATE_ROOT.resolve("employee-project-mapping-form.html"));
        assertThat(html)
                .contains("Only external projects are available for this external employee.")
                .contains("editView.availableProjects")
                .contains("Save Project Mapping")
                .contains("autocomplete=\"off\"");
    }
}
