package com.maharecruitment.gov.in.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BeanPropertyBindingResult;

import com.maharecruitment.gov.in.web.dto.verification.OtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.verification.OtpSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpFailureReason;
import com.maharecruitment.gov.in.web.service.verification.OtpResponseCodes;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

class OtpVerificationControllerTest {

    @Test
    void sendEmailOtpReturns503WhenEmailServiceIsUnavailable() {
        OtpVerificationService service = mock(OtpVerificationService.class);
        OtpVerificationController controller = new OtpVerificationController(
                service,
                false,
                new NotificationChannelProperties(),
                new TransportSecurityProperties());

        OtpSendRequest request = new OtpSendRequest();
        request.setPurpose(VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
        request.setChannel(VerificationChannel.EMAIL);
        request.setReference("user@example.com");
        when(service.sendOtp(any(), any(), any(), any(), any()))
                .thenThrow(new OtpDeliveryException(
                        VerificationChannel.EMAIL,
                        "SMTP unavailable",
                        new IllegalStateException("connection failed")));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(
                "Email OTP service is temporarily unavailable. Please try again later.",
                response.getBody().message());
    }

    @Test
    void verifyOtpReturns429WhenRateLimitExceeded() {
        OtpVerificationService service = mock(OtpVerificationService.class);
        NotificationChannelProperties notificationProperties = new NotificationChannelProperties();
        OtpVerificationController controller = new OtpVerificationController(
                service,
                false,
                notificationProperties,
                new TransportSecurityProperties());

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPurpose(VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
        request.setChannel(VerificationChannel.EMAIL);
        request.setReference("user@example.com");
        request.setOtp("123456");

        when(service.verifyOtp(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new OtpRateLimitException("rate limit", 60));

        ResponseEntity<VerificationResponse> response = controller.verifyOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(60, response.getBody().retryAfterSeconds());
        assertEquals(OtpResponseCodes.OTP_RATE_LIMITED, response.getBody().code());
        assertEquals(false, response.getBody().success());
    }

    @Test
    void fifthFailureReturnsStableAttemptLimitCodeAndMessage() {
        OtpVerificationService service = mock(OtpVerificationService.class);
        OtpVerificationController controller = new OtpVerificationController(
                service,
                false,
                new NotificationChannelProperties(),
                new TransportSecurityProperties());
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPurpose(VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
        request.setChannel(VerificationChannel.EMAIL);
        request.setReference("user@example.com");
        request.setOtp("123456");
        when(service.verifyOtp(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new OtpVerificationException(
                        OtpFailureReason.ATTEMPTS_EXCEEDED,
                        "internal detail",
                        OtpVerificationResult.locked(java.time.Instant.now().plusSeconds(900), 900, false, null, null)));

        ResponseEntity<VerificationResponse> response = controller.verifyOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(OtpResponseCodes.OTP_ATTEMPTS_EXCEEDED, response.getBody().code());
        assertEquals(
                "Maximum OTP verification attempts exceeded. Please request a new OTP.",
                response.getBody().message());
        assertEquals(0, response.getBody().remainingAttempts());
    }

    @Test
    void sendReturnsResendLimitCodeWithHttp429() {
        OtpVerificationService service = mock(OtpVerificationService.class);
        OtpVerificationController controller = new OtpVerificationController(
                service,
                false,
                new NotificationChannelProperties(),
                new TransportSecurityProperties());
        OtpSendRequest request = new OtpSendRequest();
        request.setPurpose(VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
        request.setChannel(VerificationChannel.EMAIL);
        request.setReference("user@example.com");
        when(service.sendOtp(any(), any(), any(), any(), any()))
                .thenThrow(new OtpRateLimitException(
                        "limit",
                        600,
                        OtpResponseCodes.OTP_RESEND_LIMIT_EXCEEDED));

        ResponseEntity<VerificationResponse> response = controller.sendOtp(
                request,
                new BeanPropertyBindingResult(request, "request"),
                new MockHttpServletRequest(),
                new MockHttpSession());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(OtpResponseCodes.OTP_RESEND_LIMIT_EXCEEDED, response.getBody().code());
        assertEquals("Maximum OTP resend limit reached. Please try again later.", response.getBody().message());
    }
}
