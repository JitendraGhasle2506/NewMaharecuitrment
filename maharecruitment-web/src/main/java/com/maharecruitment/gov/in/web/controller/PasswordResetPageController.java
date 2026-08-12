package com.maharecruitment.gov.in.web.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetResponse;
import com.maharecruitment.gov.in.web.dto.passwordreset.ResetPasswordRequest;
import com.maharecruitment.gov.in.web.properties.PasswordResetProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetException;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetService;
import com.maharecruitment.gov.in.web.service.passwordreset.ResetPasswordChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class PasswordResetPageController {

    private static final String IDENTIFIER_SESSION_KEY = "passwordReset.identifier";
    private static final String RESET_TOKEN_SESSION_KEY = "passwordReset.resetToken";
    private static final String OTP_EXPIRY_SESSION_KEY = "passwordReset.otpExpiresAt";

    private final PasswordResetService passwordResetService;
    private final PasswordResetProperties passwordResetProperties;
    private final TransportSecurityProperties transportSecurityProperties;

    public PasswordResetPageController(
            PasswordResetService passwordResetService,
            PasswordResetProperties passwordResetProperties,
            TransportSecurityProperties transportSecurityProperties) {
        this.passwordResetService = passwordResetService;
        this.passwordResetProperties = passwordResetProperties;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("otpRequest")) {
            model.addAttribute("otpRequest", new PasswordResetOtpRequest());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password/request-otp")
    public String requestOtp(
            @Valid @ModelAttribute("otpRequest") PasswordResetOtpRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "forgot-password";
        }
        try {
            PasswordResetResponse response = passwordResetService.requestOtp(
                    request,
                    ResetPasswordChannel.WEB,
                    clientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));
            session.setAttribute(IDENTIFIER_SESSION_KEY, request.getIdentifier());
            rememberOtpExpiry(session);
            session.removeAttribute(RESET_TOKEN_SESSION_KEY);
            redirectAttributes.addFlashAttribute("passwordResetMessage", response.getMessage());
            return "redirect:/forgot-password/verify";
        } catch (PasswordResetException ex) {
            model.addAttribute("passwordResetError", ex.getMessage());
            model.addAttribute("passwordResetErrorCode", ex.getCode());
            model.addAttribute("passwordResetRetryAfterSeconds", ex.getRetryAfterSeconds());
            return "forgot-password";
        }
    }

    @GetMapping("/forgot-password/verify")
    public String verifyOtp(HttpSession session, Model model) {
        if (!StringUtils.hasText(sessionIdentifier(session))) {
            return "redirect:/forgot-password";
        }
        populateOtpTiming(model, session);
        return "verify-password-reset-otp";
    }

    @PostMapping("/forgot-password/resend-otp")
    public String resendOtp(
            HttpServletRequest httpRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String identifier = sessionIdentifier(session);
        if (!StringUtils.hasText(identifier)) {
            return "redirect:/forgot-password";
        }

        PasswordResetOtpRequest request = new PasswordResetOtpRequest();
        request.setIdentifier(identifier);
        try {
            PasswordResetResponse response = passwordResetService.requestOtp(
                    request,
                    ResetPasswordChannel.WEB,
                    clientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));
            session.removeAttribute(RESET_TOKEN_SESSION_KEY);
            rememberOtpExpiry(session);
            redirectAttributes.addFlashAttribute("passwordResetMessage", response.getMessage());
        } catch (PasswordResetException ex) {
            redirectAttributes.addFlashAttribute("passwordResetError", ex.getMessage());
            redirectAttributes.addFlashAttribute("passwordResetErrorCode", ex.getCode());
            redirectAttributes.addFlashAttribute("passwordResetRetryAfterSeconds", ex.getRetryAfterSeconds());
        }
        return "redirect:/forgot-password/verify";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String verifyOtp(
            @RequestParam("otp") String otp,
            HttpServletRequest httpRequest,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        String identifier = sessionIdentifier(session);
        if (!StringUtils.hasText(identifier)) {
            return "redirect:/forgot-password";
        }
        if (!StringUtils.hasText(otp) || !otp.matches("\\d{6}")) {
            populateOtpTiming(model, session);
            model.addAttribute("passwordResetError", "OTP must be exactly 6 digits.");
            return "verify-password-reset-otp";
        }

        PasswordResetOtpVerifyRequest request = new PasswordResetOtpVerifyRequest();
        request.setIdentifier(identifier);
        request.setOtp(otp);
        try {
            PasswordResetResponse response = passwordResetService.verifyOtp(
                    request,
                    ResetPasswordChannel.WEB,
                    clientIp(httpRequest));
            session.setAttribute(RESET_TOKEN_SESSION_KEY, response.getResetToken());
            redirectAttributes.addFlashAttribute("passwordResetMessage", "OTP verified successfully.");
            return "redirect:/forgot-password/reset";
        } catch (PasswordResetException ex) {
            populateOtpTiming(model, session);
            model.addAttribute("passwordResetError", ex.getMessage());
            model.addAttribute("passwordResetErrorCode", ex.getCode());
            model.addAttribute("passwordResetRetryAfterSeconds", ex.getRetryAfterSeconds());
            return "verify-password-reset-otp";
        }
    }

    @GetMapping("/forgot-password/reset")
    public String resetPassword(HttpSession session, Model model) {
        if (!StringUtils.hasText(sessionResetToken(session))) {
            return "redirect:/forgot-password";
        }
        if (!model.containsAttribute("resetRequest")) {
            model.addAttribute("resetRequest", new ResetPasswordRequest());
        }
        return "reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(
            @ModelAttribute("resetRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        String resetToken = sessionResetToken(session);
        if (!StringUtils.hasText(resetToken)) {
            return "redirect:/forgot-password";
        }
        request.setResetToken(resetToken);
        validateResetForm(request, bindingResult);
        if (bindingResult.hasErrors()) {
            clearPasswordFields(request);
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(request, ResetPasswordChannel.WEB, clientIp(httpRequest));
            clearResetSession(session);
            redirectAttributes.addFlashAttribute("passwordResetMessage", "Password reset successfully.");
            return "redirect:/forgot-password/success";
        } catch (PasswordResetException ex) {
            clearPasswordFields(request);
            model.addAttribute("passwordResetError", ex.getMessage());
            return "reset-password";
        }
    }

    @GetMapping("/forgot-password/success")
    public String success() {
        return "password-reset-success";
    }

    private void validateResetForm(ResetPasswordRequest request, BindingResult bindingResult) {
        if (!StringUtils.hasText(request.getNewPassword())) {
            bindingResult.rejectValue("newPassword", "password.required", "New password is required.");
        }
        if (!StringUtils.hasText(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.confirm.required", "Confirm password is required.");
        }
        if (StringUtils.hasText(request.getNewPassword())
                && StringUtils.hasText(request.getConfirmPassword())
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.confirm", "New password and confirm password must match.");
        }
    }

    private void populateOtpTiming(Model model, HttpSession session) {
        model.addAttribute("otpExpirySeconds", remainingOtpSeconds(session));
        model.addAttribute("otpResendCooldownSeconds", passwordResetProperties.getResendCooldownSeconds());
    }

    private void rememberOtpExpiry(HttpSession session) {
        session.setAttribute(
                OTP_EXPIRY_SESSION_KEY,
                Instant.now().plusSeconds(passwordResetProperties.getOtpValiditySeconds()));
    }

    private long remainingOtpSeconds(HttpSession session) {
        Object expiry = session.getAttribute(OTP_EXPIRY_SESSION_KEY);
        if (!(expiry instanceof Instant expiresAt)) {
            return passwordResetProperties.getOtpValiditySeconds();
        }
        return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
    }

    private String clientIp(HttpServletRequest request) {
        return OtpRequestContext.from(request, transportSecurityProperties.isTrustForwardedHeaders())
                .normalizedClientIp();
    }

    private String sessionIdentifier(HttpSession session) {
        Object identifier = session.getAttribute(IDENTIFIER_SESSION_KEY);
        return identifier instanceof String value ? value : null;
    }

    private String sessionResetToken(HttpSession session) {
        Object resetToken = session.getAttribute(RESET_TOKEN_SESSION_KEY);
        return resetToken instanceof String value ? value : null;
    }

    private void clearResetSession(HttpSession session) {
        session.removeAttribute(IDENTIFIER_SESSION_KEY);
        session.removeAttribute(RESET_TOKEN_SESSION_KEY);
        session.removeAttribute(OTP_EXPIRY_SESSION_KEY);
    }

    private void clearPasswordFields(ResetPasswordRequest request) {
        request.setNewPassword(null);
        request.setConfirmPassword(null);
    }
}
