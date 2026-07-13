package com.maharecruitment.gov.in.common.sms.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SmsTemplateServiceTest {

    @Test
    void loginOtpTemplateOutputMatchesDltTextExactly() {
        SmsTemplateService service = new SmsTemplateService(new SmsTemplateProperties());

        String message = service.buildLoginOtpMessage("930712");

        assertThat(message).isEqualTo(
                "OTP for Maharecruitment Portal login: 930712. "
                        + "Valid for 10 minutes. "
                        + "Do not share this OTP with anyone. - MAHGOV");
        assertThat(message).contains("930712. Valid");
    }

    @Test
    void loginOtpRequiresExactlySixDigits() {
        SmsTemplateService service = new SmsTemplateService(new SmsTemplateProperties());

        assertThatThrownBy(() -> service.buildLoginOtpMessage("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("six numeric digits");
        assertThatThrownBy(() -> service.buildLoginOtpMessage("12345A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("six numeric digits");
    }

    @Test
    void genericTemplateRejectsExtraParametersAndSelectsConfiguredAppId() {
        SmsTemplateProperties properties = new SmsTemplateProperties();
        SmsTemplateProperties.Template template = new SmsTemplateProperties.Template();
        template.setAppId("APP-SUBMITTED");
        template.setMessage("Application {application_id} submitted for {candidate_name}");
        template.setRequiredParameters(Set.of("application_id", "candidate_name"));
        properties.setTemplates(new LinkedHashMap<>(Map.of("application-submitted", template)));

        SmsTemplateService service = new SmsTemplateService(properties);

        assertThat(service.getTemplateDefinition(SmsTemplateCode.APPLICATION_SUBMITTED).appId())
                .isEqualTo("APP-SUBMITTED");
        assertThat(service.buildTemplateMessage(
                SmsTemplateCode.APPLICATION_SUBMITTED,
                Map.of("application_id", "A-100", "candidate_name", "Candidate")))
                .isEqualTo("Application A-100 submitted for Candidate");
        assertThatThrownBy(() -> service.buildTemplateMessage(
                SmsTemplateCode.APPLICATION_SUBMITTED,
                Map.of("application_id", "A-100", "candidate_name", "Candidate", "otp", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected SMS template parameters");
    }
}
