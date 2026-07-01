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
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

class OtpVerificationControllerTest {

    @Test
    void verifyOtpReturns429WhenRateLimitExceeded() {
        OtpVerificationService service = mock(OtpVerificationService.class);
        OtpVerificationController controller = new OtpVerificationController(
                service,
                false,
                true,
                true,
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
    }
}
