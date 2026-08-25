package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AgencyRecruitmentNotificationCandidateTemplateTest {

    @Test
    void candidateFormCollectsCtcAndConditionalResignationDetails() throws IOException {
        String template = new ClassPathResource("templates/agency/recruitment-notification-detail.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String script = new ClassPathResource("static/js/agency-notification-candidates.js")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Current CTC (₹)")
                .contains("].currentCtc}")
                .contains("].resigned}")
                .contains("].lastWorkingDay}")
                .contains("class=\"form-control last-working-day-input\"")
                .contains("th:disabled=\"${candidateRow.resigned != true}\"")
                .contains("Min Exp: ")
                .contains("+ yrs")
                .contains("agency-notification-candidates.js(v=6)")
                .doesNotContain("data-max-exp");
        assertThat(script)
                .contains("name=\"candidates[' + index + '].currentCtc\"")
                .contains("name=\"candidates[' + index + '].resigned\"")
                .contains("name=\"candidates[' + index + '].lastWorkingDay\"")
                .contains("lastWorkingDayInput.required = resigned")
                .contains("lastWorkingDayInput.disabled = !resigned")
                .contains("Minimum total experience: ")
                .contains("Candidates with higher experience are allowed.")
                .doesNotContain("must not exceed")
                .doesNotContain("data-max-exp");
    }
}
