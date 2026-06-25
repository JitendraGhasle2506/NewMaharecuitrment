package com.maharecruitment.gov.in.web.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler;
import com.maharecruitment.gov.in.web.dto.login.OtpLoginForm;
import com.maharecruitment.gov.in.web.dto.login.OtpLoginSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.login.OtpLoginService;
import com.maharecruitment.gov.in.web.service.login.UnknownLoginIdentifierException;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class OtpLoginController {

    private static final String GENERIC_VERIFY_FAILURE = "OTP verification failed. Please try again.";
    private static final String GENERIC_SEND_ACCEPTED =
            "OTP request accepted. If the account details are valid, an OTP will be sent.";
    private static final String GENERIC_SEND_VALIDATION_FAILURE = "Please enter required OTP login details.";
    private static final String RATE_LIMIT_MESSAGE = "Too many OTP requests. Please try again later.";

    private final OtpLoginService otpLoginService;
    private final MySimpleUrlAuthenticationSuccessHandler successHandler;

    public OtpLoginController(
            OtpLoginService otpLoginService,
            MySimpleUrlAuthenticationSuccessHandler successHandler) {
        this.otpLoginService = otpLoginService;
        this.successHandler = successHandler;
    }

    @PostMapping("/login/otp/send")
    @ResponseBody
    public ResponseEntity<VerificationResponse> sendOtp(
            @Valid @RequestBody OtpLoginSendRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    validationMessage(bindingResult),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        }

        if (!otpLoginService.isChannelEnabled(request.getChannel())) {
            return ResponseEntity.ok(new VerificationResponse(
                    otpLoginService.disabledChannelMessage(request.getChannel()),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        }

        try {
            OtpVerificationResult result = otpLoginService.sendOtp(
                    session,
                    request.getIdentifier(),
                    request.getChannel(),
                    OtpRequestContext.from(httpRequest));
            return ResponseEntity.ok(toResponse(
                    GENERIC_SEND_ACCEPTED,
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel(),
                    result));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(toResponse(
                    RATE_LIMIT_MESSAGE,
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel(),
                    ex.getResult()));
        } catch (UnknownLoginIdentifierException ex) {
            return ResponseEntity.ok(new VerificationResponse(
                    ex.getMessage(),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    GENERIC_SEND_ACCEPTED,
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        }
    }

    @PostMapping("/login/otp")
    public void loginWithOtp(
            @Valid @ModelAttribute OtpLoginForm form,
            BindingResult bindingResult,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (bindingResult.hasErrors()) {
            redirectWithFlash(
                    request,
                    response,
                    GENERIC_VERIFY_FAILURE,
                    form,
                    null);
            return;
        }

        try {
            Authentication authentication = otpLoginService.authenticate(
                    session,
                    form.getIdentifier(),
                    form.getChannel(),
                    form.getOtp(),
                    form.getCaptchaId(),
                    form.getCaptchaAnswer(),
                    OtpRequestContext.from(request));
            request.changeSessionId();
            successHandler.onAuthenticationSuccess(request, response, authentication);
        } catch (OtpVerificationException ex) {
            redirectWithFlash(request, response, GENERIC_VERIFY_FAILURE, form, ex.getResult());
        } catch (RuntimeException ex) {
            redirectWithFlash(request, response, GENERIC_VERIFY_FAILURE, form, null);
        }
    }

    private void redirectWithFlash(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorMessage,
            OtpLoginForm form,
            OtpVerificationResult result) throws IOException {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("otpErrorMessage", errorMessage);
        flashMap.put("otpIdentifier", form.getIdentifier());
        flashMap.put("otpChannel", form.getChannel() != null ? form.getChannel().name() : "");
        if (result != null) {
            flashMap.put("otpRemainingAttempts", result.remainingAttempts());
            flashMap.put("otpCaptchaRequired", result.captchaRequired());
            flashMap.put("otpCaptchaId", result.captchaId());
            flashMap.put("otpCaptchaQuestion", result.captchaQuestion());
            flashMap.put("otpLockSecondsRemaining", result.lockSecondsRemaining());
            flashMap.put("otpLockedUntil", result.lockedUntil() == null ? null : result.lockedUntil().toString());
        }

        if (RequestContextUtils.getFlashMapManager(request) != null) {
            RequestContextUtils.getFlashMapManager(request).saveOutputFlashMap(flashMap, request, response);
        }

        response.sendRedirect(request.getContextPath() + "/login");
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

    private String validationMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(GENERIC_SEND_VALIDATION_FAILURE);
    }
}
