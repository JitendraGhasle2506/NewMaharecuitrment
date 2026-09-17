package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PartialPaymentAuthorizationTemplateTest {

    @Test
    void toggleSubmitsCheckedStateWithoutCspBlockedInlineHandler() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("templates/hr/advance-payment-authorization.html");
        String template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("class=\"partial-payment-authorization-form\"")
                .contains("class=\"partial-payment-toggle\"")
                .contains("allowedInput.value = String(toggle.checked)")
                .contains("toggle.addEventListener('change'")
                .contains("form.requestSubmit()")
                .doesNotContain("onchange=\"this.form.submit()\"");
    }
}
