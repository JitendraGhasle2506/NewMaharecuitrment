package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetResponse;
import com.maharecruitment.gov.in.web.dto.passwordreset.ResetPasswordRequest;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetService;
import com.maharecruitment.gov.in.web.service.passwordreset.ResetPasswordChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/auth/password-reset")
public class MobilePasswordResetController {

    private final PasswordResetService passwordResetService;
    private final TransportSecurityProperties transportSecurityProperties;

    public MobilePasswordResetController(
            PasswordResetService passwordResetService,
            TransportSecurityProperties transportSecurityProperties) {
        this.passwordResetService = passwordResetService;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @PostMapping(value = "/request-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PasswordResetResponse> requestOtp(
            @Valid @RequestBody PasswordResetOtpRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(passwordResetService.requestOtp(
                request,
                ResetPasswordChannel.MOBILE_API,
                clientIp(httpRequest),
                httpRequest.getHeader("User-Agent")));
    }

    @PostMapping(value = "/verify-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PasswordResetResponse> verifyOtp(
            @Valid @RequestBody PasswordResetOtpVerifyRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(passwordResetService.verifyOtp(
                request,
                ResetPasswordChannel.MOBILE_API,
                clientIp(httpRequest)));
    }

    @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(passwordResetService.resetPassword(
                request,
                ResetPasswordChannel.MOBILE_API,
                clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        return OtpRequestContext.from(request, transportSecurityProperties.isTrustForwardedHeaders())
                .normalizedClientIp();
    }
}
