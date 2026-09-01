package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EmployeeDashboardMarriageDetailsTemplateTest {

    @Test
    void marriageDetailsAreConditionallyAvailableForMarriedEmployees() throws IOException {
        String dashboard = resource("templates/employee/dashboard.html");
        String template = resource("templates/employee/profile-update.html");
        String script = resource("static/js/employee-dashboard.js");

        assertThat(dashboard)
                .doesNotContain("Editable Details", "Personal Information", "id=\"employeeProfileForm\"");
        assertThat(template)
                .contains("id=\"maritalStatus\" name=\"maritalStatus\"")
                .contains("id=\"marriageDetails\" class=\"marriage-details\"")
                .contains("th:hidden=\"${profile.maritalStatus != 'Married'}\"")
                .contains("name=\"spouseName\"")
                .contains("th:value=\"${profile.spouseName}\"")
                .contains("name=\"marriageDate\" type=\"date\"")
                .contains("th:value=\"${profile.marriageDate}\"");
        assertThat(script)
                .contains("maritalStatus?.addEventListener('change', updateMarriageDetails)")
                .contains("marriageDetails.hidden = !married")
                .contains("input.disabled = !married")
                .contains("Marriage date cannot be in the future");
    }

    private String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
