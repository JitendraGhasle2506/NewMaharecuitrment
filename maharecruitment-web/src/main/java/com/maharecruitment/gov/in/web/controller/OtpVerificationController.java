package com.maharecruitment.gov.in.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.common.sms.exception.SmsGatewayException;
import com.maharecruitment.gov.in.web.dto.verification.OtpSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.OtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/verifications/otp")
public class OtpVerificationController {

    private static final String GENERIC_VERIFY_FAILURE = "OTP verification failed. Please try again.";
    private static final String GENERIC_SEND_FAILURE = "Unable to process OTP request. Please try again.";
    private static final String SMS_SEND_FAILURE = "Unable to send OTP at this time. Please try again later.";
    private static final String EMAIL_SEND_FAILURE =
            "Email OTP service is temporarily unavailable. Please try again later.";
    private static final String DELIVERY_SEND_FAILURE =
            "OTP delivery service is temporarily unavailable. Please try again later.";
    private static final String RATE_LIMIT_MESSAGE = "Too many OTP requests. Please try again later.";

    private final OtpVerificationService otpVerificationService;
    private final boolean departmentRegistrationOtpBypassEnabled;
    private final NotificationChannelProperties notificationChannelProperties;
    private final TransportSecurityProperties transportSecurityProperties;

    public OtpVerificationController(
            OtpVerificationService otpVerificationService,
            @Value("${registration.department.otp-bypass-enabled:false}") boolean departmentRegistrationOtpBypassEnabled,
            NotificationChannelProperties notificationChannelProperties,
            TransportSecurityProperties transportSecurityProperties) {
        this.otpVerificationService = otpVerificationService;
        this.departmentRegistrationOtpBypassEnabled = departmentRegistrationOtpBypassEnabled;
        this.notificationChannelProperties = notificationChannelProperties;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @PostMapping("/send")
    public ResponseEntity<VerificationResponse> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_SEND_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
        if (isDepartmentRegistrationOtpDisabled(request.getPurpose(), request.getChannel())) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    buildDepartmentRegistrationOtpDisabledMessage(request.getChannel()),
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
        if (isDepartmentRegistrationOtpBypassed(request.getPurpose())) {
            return ResponseEntity.ok(new VerificationResponse(
                    "OTP verification is bypassed for department registration in this environment.",
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }

        try {
            OtpVerificationResult result = otpVerificationService.sendOtp(
                    session,
                    request.getPurpose(),
                    request.getChannel(),
                    request.getReference(),
                    OtpRequestContext.from(httpRequest, transportSecurityProperties.isTrustForwardedHeaders()));
            return ResponseEntity.ok(toResponse(
                    "OTP sent successfully.",
                    false,
                    request.getPurpose(),
                    request.getChannel(),
                    result));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(toResponse(
                    RATE_LIMIT_MESSAGE,
                    false,
                    request.getPurpose(),
                    request.getChannel(),
                    ex.getResult()));
        } catch (SmsGatewayException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerificationResponse(
                    SMS_SEND_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        } catch (OtpDeliveryException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerificationResponse(
                    deliveryFailureMessage(ex),
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_SEND_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_VERIFY_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
        if (isDepartmentRegistrationOtpDisabled(request.getPurpose(), request.getChannel())) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    buildDepartmentRegistrationOtpDisabledMessage(request.getChannel()),
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
        if (isDepartmentRegistrationOtpBypassed(request.getPurpose())) {
            return ResponseEntity.ok(new VerificationResponse(
                    "OTP verification is bypassed for department registration in this environment.",
                    true,
                    request.getPurpose(),
                    request.getChannel()));
        }

        try {
            OtpVerificationResult result = otpVerificationService.verifyOtp(
                    session,
                    request.getPurpose(),
                    request.getChannel(),
                    request.getReference(),
                    request.getOtp(),
                    request.getCaptchaId(),
                    request.getCaptchaAnswer(),
                    OtpRequestContext.from(httpRequest, transportSecurityProperties.isTrustForwardedHeaders()));
            return ResponseEntity.ok(toResponse(
                    "OTP verified successfully.",
                    true,
                    request.getPurpose(),
                    request.getChannel(),
                    result));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(toResponse(
                    RATE_LIMIT_MESSAGE,
                    false,
                    request.getPurpose(),
                    request.getChannel(),
                    ex.getResult()));
        } catch (OtpVerificationException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_VERIFY_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel(),
                    ex.getResult().remainingAttempts(),
                    ex.getResult().captchaRequired(),
                    ex.getResult().captchaId(),
                    ex.getResult().captchaQuestion(),
                    ex.getResult().lockedUntil() == null ? null : ex.getResult().lockedUntil().toString(),
                    ex.getResult().lockSecondsRemaining(),
                    ex.getResult().remainingResends(),
                    ex.getResult().retryAfterSeconds(),
                    ex.getResult().expirySeconds()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_VERIFY_FAILURE,
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
    }

    private VerificationResponse toResponse(
            String message,
            boolean verified,
            String purpose,
            com.maharecruitment.gov.in.web.dto.verification.VerificationChannel channel,
            OtpVerificationResult result) {
        return new VerificationResponse(
                message,
                verified,
                purpose,
                channel,
                result.remainingAttempts(),
                result.captchaRequired(),
                result.captchaId(),
                result.captchaQuestion(),
                result.lockedUntil() == null ? null : result.lockedUntil().toString(),
                result.lockSecondsRemaining(),
                result.remainingResends(),
                result.retryAfterSeconds(),
                result.expirySeconds(),
                result.deliveryChannel() == null ? channel.name() : result.deliveryChannel().name(),
                result.maskedDestination(),
                result.expirySeconds(),
                result.resendAvailableInSeconds());
    }

    private boolean isDepartmentRegistrationOtpBypassed(String purpose) {
        return departmentRegistrationOtpBypassEnabled
                && VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT.equalsIgnoreCase(
                        purpose == null ? "" : purpose.trim());
    }

    private boolean isDepartmentRegistrationOtpDisabled(String purpose, com.maharecruitment.gov.in.web.dto.verification.VerificationChannel channel) {
        if (!VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT.equalsIgnoreCase(
                purpose == null ? "" : purpose.trim())) {
            return false;
        }

        if (channel != null && channel.isSmsDelivery()) {
            return !departmentRegistrationOtpBypassEnabled && !notificationChannelProperties.isSmsEnabled();
        }

        if (channel == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.EMAIL) {
            return !departmentRegistrationOtpBypassEnabled && !notificationChannelProperties.isEmailEnabled();
        }

        return false;
    }

    private String buildDepartmentRegistrationOtpDisabledMessage(
            com.maharecruitment.gov.in.web.dto.verification.VerificationChannel channel) {
        return channel != null && channel.isSmsDelivery()
                ? "Mobile OTP is disabled for department registration in this environment."
                : "Email OTP is disabled for department registration in this environment.";
    }

    private String deliveryFailureMessage(OtpDeliveryException exception) {
        return exception.getChannel() == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.EMAIL
                ? EMAIL_SEND_FAILURE
                : DELIVERY_SEND_FAILURE;
    }
}
