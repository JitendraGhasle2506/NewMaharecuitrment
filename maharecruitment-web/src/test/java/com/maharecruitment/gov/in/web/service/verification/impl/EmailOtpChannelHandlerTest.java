package com.maharecruitment.gov.in.web.service.verification.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;

class EmailOtpChannelHandlerTest {

    @Test
    void classifiesEmailInfrastructureFailureAsDeliveryFailure() {
        OtpDispatchService dispatchService = mock(OtpDispatchService.class);
        EmailOtpChannelHandler handler = new EmailOtpChannelHandler(dispatchService);
        IllegalStateException smtpFailure = new IllegalStateException("SMTP connection failed");
        doThrow(smtpFailure).when(dispatchService).sendEmailOtp(
                "user@example.com",
                "123456",
                "login-authentication",
                "OTP-ABC123");

        assertThatThrownBy(() -> handler.dispatchOtp(
                "login-authentication",
                "user@example.com",
                "123456",
                "OTP-ABC123"))
                .isInstanceOfSatisfying(OtpDeliveryException.class, exception -> {
                    assertThat(exception.getChannel()).isEqualTo(VerificationChannel.EMAIL);
                    assertThat(exception.getCause()).isSameAs(smtpFailure);
                });
    }
}
