package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.login.OtpLoginService;
import com.maharecruitment.gov.in.web.service.login.UnknownLoginIdentifierException;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;

class OtpLoginControllerTest {

    private final OtpLoginService otpLoginService = mock(OtpLoginService.class);
    private final OtpLoginController controller = new OtpLoginController(
            otpLoginService,
            mock(MySimpleUrlAuthenticationSuccessHandler.class),
            new TransportSecurityProperties());

    @Test
    void disabledOtpChannelReturnsOkMessageWithoutSendingOtp() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("hr@mahait.org");
        request.setChannel("EMAIL");
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
        request.setChannel("EMAIL");
        when(otpLoginService.isChannelEnabled(VerificationChannel.EMAIL)).thenReturn(true);
        when(otpLoginService.sendOtp(any(), any(), any(), any()))
                .thenThrow(new UnknownLoginIdentifierException(
                        "Email or mobile number is not registered."));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Email or mobile number is not registered.");
        assertThat(response.getBody().expirySeconds()).isZero();
    }

    @Test
    void missingOtpChannelInfersEmailFromIdentifier() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("hr@mahait.org");
        when(otpLoginService.isChannelEnabled(VerificationChannel.EMAIL)).thenReturn(true);
        when(otpLoginService.sendOtp(any(), any(), any(), any()))
                .thenReturn(OtpVerificationResult.sent(VerificationChannel.EMAIL, "h***@mahait.org", 2, 600, 60));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().channel()).isEqualTo(VerificationChannel.EMAIL);
        verify(otpLoginService).sendOtp(any(), eq("hr@mahait.org"), eq(VerificationChannel.EMAIL), any());
    }

    @Test
    void mobileIdentifierInfersSmsChannelEvenWhenPostedChannelIsEmail() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("9876543210");
        request.setChannel("EMAIL");
        when(otpLoginService.isChannelEnabled(VerificationChannel.SMS)).thenReturn(true);
        when(otpLoginService.sendOtp(any(), any(), any(), any()))
                .thenReturn(OtpVerificationResult.sent(VerificationChannel.SMS, "******3210", 2, 600, 60));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().channel()).isEqualTo(VerificationChannel.SMS);
        verify(otpLoginService).sendOtp(any(), eq("9876543210"), eq(VerificationChannel.SMS), any());
    }

    @Test
    void activeOtpRateLimitTellsUserToEnterLatestValidOtp() {
        OtpLoginSendRequest request = new OtpLoginSendRequest();
        request.setIdentifier("hr@mahait.org");
        request.setChannel("EMAIL");
        when(otpLoginService.isChannelEnabled(VerificationChannel.EMAIL)).thenReturn(true);
        when(otpLoginService.sendOtp(any(), any(), any(), any()))
                .thenThrow(new OtpRateLimitException(
                        "OTP is already valid. Resend is allowed after it expires.",
                        240));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("OTP already sent. Please enter the latest valid OTP. Resend is available after the timer ends.");
        assertThat(response.getBody().retryAfterSeconds()).isEqualTo(240);
    }
}
