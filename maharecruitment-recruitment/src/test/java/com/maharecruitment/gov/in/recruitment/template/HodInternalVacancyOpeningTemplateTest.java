package com.maharecruitment.gov.in.recruitment.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HodInternalVacancyOpeningTemplateTest {

    @Test
    void formConditionallyCollectsApprovalOrReplacementEmployee() throws IOException {
        String template = new ClassPathResource(
                "templates/hod/internal-vacancy-opening-form.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("enctype=\"multipart/form-data\"")
                .contains("th:action=\"@{/hod1/internal-vacancies(_csrf=${_csrf.token})}\"")
                .contains("th:if=\"${_csrf != null}\"")
                .contains("th:name=\"${_csrf.parameterName}\"")
                .contains("th:value=\"${_csrf.token}\"")
                .contains("th:field=\"*{projectId}\" required")
                .contains("Please select a project.")
                .contains("id=\"internalVacancyClientValidationSummary\"")
                .contains("th:field=\"*{hiringRequestType}\" value=\"NEW_CANDIDATE\"")
                .contains("th:field=\"*{hiringRequestType}\" value=\"EMPLOYEE_REPLACEMENT\"")
                .contains("th:field=\"*{eOfficeApprovalDocument}\"")
                .contains("accept=\"application/pdf,.pdf\"")
                .contains("name=\"replacementEmployeeIds\"")
                .contains("th:checked=\"${openingForm.replacementEmployeeIds != null and openingForm.replacementEmployeeIds.contains(employee.employeeId)}\"")
                .contains("replacement-employee-checkbox")
                .contains("th:each=\"employee : ${replacementEmployeeOptions}\"")
                .contains("data-designation-id=${employee.designationId}")
                .contains("data-level-code=${employee.levelCode}")
                .contains("id=\"replacementEmployeeSearch\"")
                .contains("Search by employee, designation or level")
                .contains("id=\"replacementEmployeeNoResults\"")
                .contains("id=\"replacementEmployeeClearSelection\"")
                .contains("id=\"replacementEmployeeCountBadge\"")
                .contains("@{/js/internal-vacancy-replacement.js(v='20260825-4')}")
                .contains("<td class=\"requirementActionColumn\">")
                .contains("approvalDocument.required = !isReplacement && !hasExistingApproval")
                .contains("value=\"draft\" class=\"btn btn-outline-primary\" formnovalidate")
                .doesNotContain("employee.employeeCode")
                .doesNotContain("th:field=\"*{replacementEmployeeIds}\"");
    }
}
