package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DepartmentAdvancePaymentReviewTemplateTest {

    @Test
    void auditorDecisionUsesCspCompatibleFormSubmission() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("templates/department/advance-payment-review.html");
        String template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("type=\"submit\" id=\"reviewSubmitButton\"")
                .contains("reviewForm.addEventListener('submit'")
                .contains("th:data-review-url=\"@{/auditor/department/payment/{paymentId}/review")
                .contains("submitButton.disabled = true")
                .doesNotContain("onclick=\"submitReview()\"");
    }
}
