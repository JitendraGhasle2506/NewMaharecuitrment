package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class HREmployeeListTemplateTest {

    private static final Path EMPLOYEE_LIST_TEMPLATE = Path.of(
            "src/main/resources/templates/hr/employee-list.html");

    @Test
    void employeeRowsExposeViewWithoutLocationOrCellActions() throws Exception {
        String template = Files.readString(EMPLOYEE_LIST_TEMPLATE);
        int tableBodyStart = template.indexOf("<tbody>");
        int tableBodyEnd = template.indexOf("</tbody>", tableBodyStart);
        String employeeRows = template.substring(tableBodyStart, tableBodyEnd);

        assertThat(employeeRows)
                .contains("@{/hr/employees/{employeeId}")
                .contains("employee-view-link")
                .contains("View <i class=\"fa-solid fa-arrow-right\"")
                .doesNotContain("emp.employeeCode")
                .doesNotContain("employee-location-mappings")
                .doesNotContain("employee-cell-mappings");
    }

    @Test
    void employeeListShowsCompactDirectoryFiltersAndActions() throws Exception {
        String template = Files.readString(EMPLOYEE_LIST_TEMPLATE);
        int tableHeadStart = template.indexOf("<thead");
        int tableHeadEnd = template.indexOf("</thead>", tableHeadStart);
        String tableHead = template.substring(tableHeadStart, tableHeadEnd);

        assertThat(tableHead)
                .contains("Employee")
                .contains("Organisation")
                .contains("Assignment")
                .contains("Reporting")
                .contains("Joined")
                .contains("Status")
                .contains("Action")
                .doesNotContain("Project Name");

        assertThat(template)
                .contains("employee-status-tabs")
                .contains("/hr/employees/resigned")
                .contains("id=\"employeeSearch\"")
                .contains("id=\"employeeAgencyFilter\"")
                .contains("name=\"agencyId\"")
                .contains("All agencies")
                .contains("data-auto-submit")
                .contains("agencyId=${currentAgencyId}")
                .contains("/hr/employees/export/excel")
                .contains("Export Excel")
                .contains("employee-list.css")
                .contains("employee-list.js")
                .contains("colspan=\"7\"");
    }
}
