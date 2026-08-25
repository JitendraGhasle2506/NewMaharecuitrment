package com.maharecruitment.gov.in.recruitment.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HrInternalVacancyOpeningTemplateTest {

    @Test
    void formSubmitsCsrfTokenForEveryHiringRequestType() throws IOException {
        String template = new ClassPathResource(
                "templates/hr/internal-vacancy-opening-form.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("method=\"post\"")
                .contains("enctype=\"multipart/form-data\"")
                .contains("th:if=\"${_csrf != null}\"")
                .contains("th:name=\"${_csrf.parameterName}\"")
                .contains("th:value=\"${_csrf.token}\"")
                .contains("th:field=\"*{hiringRequestType}\" value=\"NEW_CANDIDATE\"")
                .contains("th:field=\"*{hiringRequestType}\" value=\"EMPLOYEE_REPLACEMENT\"")
                .contains("name=\"replacementEmployeeIds\"")
                .contains("th:checked=\"${openingForm.replacementEmployeeIds != null and openingForm.replacementEmployeeIds.contains(employee.employeeId)}\"")
                .doesNotContain("th:field=\"*{replacementEmployeeIds}\"");
    }
}
