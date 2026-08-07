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
                .contains("fa-regular fa-eye")
                .contains("> View")
                .doesNotContain("employee-location-mappings")
                .doesNotContain("employee-cell-mappings");
    }

    @Test
    void employeeListShowsOnlyRequestedColumnsAndAgencyFilter() throws Exception {
        String template = Files.readString(EMPLOYEE_LIST_TEMPLATE);
        int tableHeadStart = template.indexOf("<thead");
        int tableHeadEnd = template.indexOf("</thead>", tableHeadStart);
        String tableHead = template.substring(tableHeadStart, tableHeadEnd);

        assertThat(tableHead)
                .contains("Employee Information")
                .contains("Employee Code")
                .contains("Agency")
                .contains("Designation")
                .contains("Mahait Joining Date")
                .contains("Type")
                .contains("Status")
                .contains("Action")
                .doesNotContain("Project Name");

        assertThat(template)
                .contains("id=\"employeeAgencyFilter\"")
                .contains("name=\"agencyId\"")
                .contains("All Agencies")
                .contains("agencyId=${currentAgencyId}")
                .contains("colspan=\"8\"");
    }
}
