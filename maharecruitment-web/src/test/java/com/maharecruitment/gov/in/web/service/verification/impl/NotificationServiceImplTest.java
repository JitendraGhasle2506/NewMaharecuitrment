package com.maharecruitment.gov.in.web.service.verification.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

class NotificationServiceImplTest {

    @Test
    void loginOtpEmailIncludesOtpAndOtpId() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationServiceImpl service = new NotificationServiceImpl(
                mailSender,
                mock(RestClient.class),
                new MockEnvironment()
                        .withProperty("spring.mail.from.email", "noreply@mahait.org")
                        .withProperty("otp.expiry-minutes", "5"),
                new NotificationChannelProperties());

        service.sendEmailOtp(
                "user@example.com",
                "209552",
                VerificationPurposes.LOGIN_AUTHENTICATION,
                "OTP-ABCD23");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("Maha Recruitment Portal Login OTP");
        assertThat(message.getText())
                .contains("209552")
                .contains("OTP ID: OTP-ABCD23")
                .contains("This OTP is valid for 5 minutes");
    }
}
