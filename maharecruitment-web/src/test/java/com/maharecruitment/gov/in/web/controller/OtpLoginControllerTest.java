package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BeanPropertyBindingResult;

import com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler;
import com.maharecruitment.gov.in.web.dto.login.OtpLoginSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.service.login.OtpLoginService;
import com.maharecruitment.gov.in.web.service.login.UnknownLoginIdentifierException;

class OtpLoginControllerTest {

    private final OtpLoginService otpLoginService = mock(OtpLoginService.class);
    private final OtpLoginController controller = new OtpLoginController(
            otpLoginService,
            mock(MySimpleUrlAuthenticationSuccessHandler.class));

    @Test
    void disabledOtpChannelReturnsOkMessageWithoutSendingOtp() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("hr@mahait.org");
        request.setChannel(VerificationChannel.EMAIL);
        when(otpLoginService.isChannelEnabled(VerificationChannel.EMAIL)).thenReturn(false);
        when(otpLoginService.disabledChannelMessage(VerificationChannel.EMAIL))
                .thenReturn("Email OTP login is not enabled in this environment.");

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Email OTP login is not enabled in this environment.");
        assertThat(response.getBody().verified()).isFalse();
        verify(otpLoginService, never()).sendOtp(any(), any(), any(), any());
    }

    @Test
    void unknownIdentifierReturnsVisibleValidationMessage() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("invalid@example.com");
        request.setChannel(VerificationChannel.EMAIL);
        when(otpLoginService.isChannelEnabled(VerificationChannel.EMAIL)).thenReturn(true);
        when(otpLoginService.sendOtp(any(), any(), any(), any()))
                .thenThrow(new UnknownLoginIdentifierException(
                        "Username, email, or mobile number is not registered."));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Username, email, or mobile number is not registered.");
        assertThat(response.getBody().expirySeconds()).isZero();
    }
}
