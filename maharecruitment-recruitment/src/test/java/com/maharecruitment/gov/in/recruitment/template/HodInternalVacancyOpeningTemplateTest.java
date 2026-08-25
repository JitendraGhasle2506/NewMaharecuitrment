package com.maharecruitment.gov.in.recruitment.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HodInternalVacancyOpeningTemplateTest {

    @Test
    void listSeparatesRequestDetailsFromSubmittedApplications() throws IOException {
        String template = new ClassPathResource(
                "templates/hod/internal-vacancy-opening-list.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("View Request")
                .contains("View Applications")
                .contains("@{/hod1/internal-vacancies/{id}/view(id=${opening.internalVacancyOpeningId})}")
                .contains("@{/hod1/internal-vacancies/request/{requestId}/applications(requestId=${opening.requestId})}");
    }

    @Test
    void candidateApplicationViewIsReadOnlyAndShowsSubmissionDetails() throws IOException {
        String template = new ClassPathResource(
                "templates/hod/internal-vacancy-candidate-list.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Candidate Applications")
                .contains("Submitted Request Details")
                .contains("requestDetails.requestId")
                .contains("requestDetails.hiringRequestTypeLabel")
                .contains("requestDetails.statusLabel")
                .contains("${requestDetails.requirements}")
                .contains("requestDetails.replacementEmployeeLabels")
                .contains("requestDetails.remarks")
                .contains("Request Submitted At")
                .contains("Submitted Candidate Applications")
                .contains("${candidateListView.candidates}")
                .contains("candidate.candidateName")
                .contains("candidate.candidateEmail")
                .contains("candidate.candidateMobile")
                .contains("candidate.candidateEducation")
                .contains("candidate.resumeFilePath")
                .contains("No candidate applications have been submitted against this request yet.")
                .doesNotContain("<form");
    }

    @Test
    void applicationViewShowsReadOnlyRequestDetails() throws IOException {
        String template = new ClassPathResource(
                "templates/hod/internal-vacancy-opening-view.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Resource Requirement Application")
                .contains("${application.requestId}")
                .contains("${application.projectName}")
                .contains("${application.requirements}")
                .contains("${application.replacementEmployeeLabels}")
                .contains("@{/hod1/internal-vacancies/{id}/e-office-approval(id=${application.internalVacancyOpeningId})}")
                .contains("application.statusCssClass")
                .contains("${application.statusLabel}")
                .contains("${application.hiringRequestTypeLabel}")
                .doesNotContain("application.status.name()")
                .doesNotContain("application.hiringRequestType.name()")
                .doesNotContain("<form");
    }

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
