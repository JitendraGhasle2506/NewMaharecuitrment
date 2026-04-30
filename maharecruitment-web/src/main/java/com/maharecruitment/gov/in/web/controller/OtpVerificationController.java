package com.maharecruitment.gov.in.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.verification.OtpSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.OtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/verifications/otp")
public class OtpVerificationController {

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
            HttpSession session) {
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
            otpVerificationService.sendOtp(
                    session,
                    request.getPurpose(),
                    request.getChannel(),
                    request.getReference());
            return ResponseEntity.ok(new VerificationResponse(
                    "OTP sent successfully.",
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    ex.getMessage(),
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpSession session) {
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
            otpVerificationService.verifyOtp(
                    session,
                    request.getPurpose(),
                    request.getChannel(),
                    request.getReference(),
                    request.getOtp());
            return ResponseEntity.ok(new VerificationResponse(
                    "OTP verified successfully.",
                    true,
                    request.getPurpose(),
                    request.getChannel()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    ex.getMessage(),
                    false,
                    request.getPurpose(),
                    request.getChannel()));
        }
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
