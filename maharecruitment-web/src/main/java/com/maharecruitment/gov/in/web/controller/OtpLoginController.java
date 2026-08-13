package com.maharecruitment.gov.in.web.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.maharecruitment.gov.in.common.security.AuthenticationAuditService;
import com.maharecruitment.gov.in.common.sms.exception.SmsGatewayException;
import com.maharecruitment.gov.in.web.dto.login.OtpLoginForm;
import com.maharecruitment.gov.in.web.dto.login.OtpLoginSendRequest;
import com.maharecruitment.gov.in.web.dto.verification.VerificationResponse;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpResponseCodes;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpLoginController.class);
    private static final String GENERIC_VERIFY_FAILURE = "OTP verification failed. Please try again.";
    private static final String GENERIC_SEND_ACCEPTED =
            "OTP request accepted. If the account details are valid, an OTP will be sent.";
    private static final String GENERIC_SEND_VALIDATION_FAILURE = "Please enter required OTP login details.";
    private static final String RATE_LIMIT_MESSAGE =
            "OTP already sent. Please enter the latest valid OTP. Resend is available after the timer ends.";
    private static final String SMS_SEND_FAILURE =
            "Unable to send OTP at this time. Please try again later.";
    private static final String EMAIL_SEND_FAILURE =
            "Email OTP service is temporarily unavailable. Please try again later.";
    private static final String DELIVERY_SEND_FAILURE =
            "OTP delivery service is temporarily unavailable. Please try again later.";

    private final OtpLoginService otpLoginService;
    private final MySimpleUrlAuthenticationSuccessHandler successHandler;
    private final TransportSecurityProperties transportSecurityProperties;
    private final AuthenticationAuditService authenticationAuditService;

    public OtpLoginController(
            OtpLoginService otpLoginService,
            MySimpleUrlAuthenticationSuccessHandler successHandler,
            TransportSecurityProperties transportSecurityProperties,
            AuthenticationAuditService authenticationAuditService) {
        this.otpLoginService = otpLoginService;
        this.successHandler = successHandler;
        this.transportSecurityProperties = transportSecurityProperties;
        this.authenticationAuditService = authenticationAuditService;
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
                    OtpRequestContext.from(httpRequest, transportSecurityProperties.isTrustForwardedHeaders()));
            return ResponseEntity.ok(toResponse(
                    GENERIC_SEND_ACCEPTED,
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel(),
                    result,
                    true,
                    OtpResponseCodes.OTP_SENT));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(toResponse(
                    rateLimitMessage(ex),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel(),
                    ex.getResult(),
                    false,
                    ex.getResponseCode()));
        } catch (OtpVerificationException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(toResponse(
                    OtpResponseCodes.messageFor(ex.getReason(), ex.getResult().remainingAttempts()),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel(),
                    ex.getResult(),
                    false,
                    OtpResponseCodes.forFailure(ex.getReason())));
        } catch (UnknownLoginIdentifierException ex) {
            return ResponseEntity.ok(new VerificationResponse(
                    ex.getMessage(),
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        } catch (SmsGatewayException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerificationResponse(
                    SMS_SEND_FAILURE,
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        } catch (OtpDeliveryException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerificationResponse(
                    deliveryFailureMessage(ex),
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
            recordOtpLoginFailure(request, form.getIdentifier(), "INVALID_REQUEST");
            redirectWithFlash(
                    request,
                    response,
                    GENERIC_VERIFY_FAILURE,
                    form,
                    null,
                    OtpResponseCodes.OTP_REQUEST_REJECTED);
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
                    OtpRequestContext.from(request, transportSecurityProperties.isTrustForwardedHeaders()));
            request.changeSessionId();
            successHandler.onAuthenticationSuccess(request, response, authentication);
        } catch (OtpVerificationException ex) {
            String failureReason = ex instanceof OtpRateLimitException rateLimitException
                    ? rateLimitException.getResponseCode()
                    : OtpResponseCodes.forFailure(ex.getReason());
            recordOtpLoginFailure(request, form.getIdentifier(), failureReason);
            redirectWithFlash(
                    request,
                    response,
                    OtpResponseCodes.messageFor(ex.getReason(), ex.getResult().remainingAttempts()),
                    form,
                    ex.getResult(),
                    failureReason);
        } catch (RuntimeException ex) {
            recordOtpLoginFailure(request, form.getIdentifier(), "AUTHENTICATION_FAILED");
            redirectWithFlash(
                    request,
                    response,
                    GENERIC_VERIFY_FAILURE,
                    form,
                    null,
                    OtpResponseCodes.OTP_REQUEST_REJECTED);
        }
    }

    private void recordOtpLoginFailure(
            HttpServletRequest request,
            String identifier,
            String failureReason) {
        try {
            authenticationAuditService.recordLoginFailure(
                    identifier,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    AuthenticationAuditService.METHOD_OTP,
                    failureReason,
                    AuthenticationAuditService.SOURCE_WEB);
        } catch (RuntimeException auditException) {
            LOGGER.error(
                    "Unable to persist failed OTP login audit. errorType={}",
                    auditException.getClass().getSimpleName(),
                    auditException);
        }
    }

    private void redirectWithFlash(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorMessage,
            OtpLoginForm form,
            OtpVerificationResult result,
            String errorCode) throws IOException {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("otpErrorMessage", errorMessage);
        flashMap.put("otpIdentifier", form.getIdentifier());
        flashMap.put("otpChannel", form.getChannel() != null ? form.getChannel().name() : "");
        flashMap.put("otpErrorCode", errorCode);
        if (result != null) {
            flashMap.put("otpRemainingAttempts", result.remainingAttempts());
            flashMap.put("otpCaptchaRequired", result.captchaRequired());
            flashMap.put("otpCaptchaId", result.captchaId());
            flashMap.put("otpCaptchaQuestion", result.captchaQuestion());
            flashMap.put("otpLockSecondsRemaining", result.lockSecondsRemaining());
            flashMap.put("otpLockedUntil", result.lockedUntil() == null ? null : result.lockedUntil().toString());
            flashMap.put("otpExpirySecondsRemaining", result.expirySeconds());
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
            OtpVerificationResult result,
            boolean success,
            String code) {
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
                result.resendAvailableInSeconds(),
                success,
                code);
    }

    private String validationMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(GENERIC_SEND_VALIDATION_FAILURE);
    }

    private String rateLimitMessage(OtpRateLimitException exception) {
        if (OtpResponseCodes.OTP_RESEND_LIMIT_EXCEEDED.equals(exception.getResponseCode())) {
            return "Maximum OTP resend limit reached. Please try again later.";
        }
        if (OtpResponseCodes.OTP_RESEND_COOLDOWN.equals(exception.getResponseCode())) {
            return RATE_LIMIT_MESSAGE;
        }
        if (exception.getMessage() != null && exception.getMessage().contains("already valid")) {
            return RATE_LIMIT_MESSAGE;
        }

        return "Too many OTP requests. Please enter the latest valid OTP or try again after the timer ends.";
    }

    private String deliveryFailureMessage(OtpDeliveryException exception) {
        return exception.getChannel() == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.EMAIL
                ? EMAIL_SEND_FAILURE
                : DELIVERY_SEND_FAILURE;
    }
}
