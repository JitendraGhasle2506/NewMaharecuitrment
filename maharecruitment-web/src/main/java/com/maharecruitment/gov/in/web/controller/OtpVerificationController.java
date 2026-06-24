package com.maharecruitment.gov.in.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.verification.OtpSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.OtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
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
    private static final String RATE_LIMIT_MESSAGE = "Too many OTP requests. Please try again later.";

    private final OtpVerificationService otpVerificationService;
    private final boolean departmentRegistrationOtpBypassEnabled;
    private final boolean departmentRegistrationMobileOtpEnabled;
    private final boolean departmentRegistrationEmailOtpEnabled;

    public OtpVerificationController(
            OtpVerificationService otpVerificationService,
            @Value("${registration.department.otp-bypass-enabled:false}") boolean departmentRegistrationOtpBypassEnabled,
            @Value("${registration.department.mobile-otp-enabled:true}") boolean departmentRegistrationMobileOtpEnabled,
            @Value("${registration.department.email-otp-enabled:true}") boolean departmentRegistrationEmailOtpEnabled) {
        this.otpVerificationService = otpVerificationService;
        this.departmentRegistrationOtpBypassEnabled = departmentRegistrationOtpBypassEnabled;
        this.departmentRegistrationMobileOtpEnabled = departmentRegistrationMobileOtpEnabled;
        this.departmentRegistrationEmailOtpEnabled = departmentRegistrationEmailOtpEnabled;
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
                    OtpRequestContext.from(httpRequest));
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
                    OtpRequestContext.from(httpRequest));
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
                result.expirySeconds());
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

        if (channel == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.MOBILE) {
            return !departmentRegistrationOtpBypassEnabled && !departmentRegistrationMobileOtpEnabled;
        }

        if (channel == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.EMAIL) {
            return !departmentRegistrationOtpBypassEnabled && !departmentRegistrationEmailOtpEnabled;
        }

        return false;
    }

    private String buildDepartmentRegistrationOtpDisabledMessage(
            com.maharecruitment.gov.in.web.dto.verification.VerificationChannel channel) {
        return channel == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.MOBILE
                ? "Mobile OTP is disabled for department registration in this environment."
                : "Email OTP is disabled for department registration in this environment.";
    }
}
