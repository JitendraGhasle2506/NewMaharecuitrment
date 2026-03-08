package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.verification.OtpSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.OtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/verifications/otp")
public class OtpVerificationController {

    private final OtpVerificationService otpVerificationService;

    public OtpVerificationController(OtpVerificationService otpVerificationService) {
        this.otpVerificationService = otpVerificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<VerificationResponse> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            HttpSession session) {
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
}
