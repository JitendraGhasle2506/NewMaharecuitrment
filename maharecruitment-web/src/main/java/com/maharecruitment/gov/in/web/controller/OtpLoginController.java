package com.maharecruitment.gov.in.web.controller;

import java.io.IOException;

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
import com.maharecruitment.gov.in.web.service.login.OtpLoginService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class OtpLoginController {

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
            HttpSession session) {
        try {
            otpLoginService.sendOtp(session, request.getIdentifier(), request.getChannel());
            return ResponseEntity.ok(new VerificationResponse(
                    request.getChannel() == com.maharecruitment.gov.in.web.dto.verification.VerificationChannel.EMAIL
                            ? "OTP sent to your registered email address."
                            : "OTP sent to your registered mobile number.",
                    false,
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    request.getChannel()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new VerificationResponse(
                    ex.getMessage(),
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
                    bindingResult.getAllErrors().get(0).getDefaultMessage(),
                    form);
            return;
        }

        try {
            Authentication authentication = otpLoginService.authenticate(
                    session,
                    form.getIdentifier(),
                    form.getChannel(),
                    form.getOtp());
            request.changeSessionId();
            successHandler.onAuthenticationSuccess(request, response, authentication);
        } catch (RuntimeException ex) {
            redirectWithFlash(request, response, ex.getMessage(), form);
        }
    }

    private void redirectWithFlash(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorMessage,
            OtpLoginForm form) throws IOException {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("otpErrorMessage", errorMessage);
        flashMap.put("otpIdentifier", form.getIdentifier());
        flashMap.put("otpChannel", form.getChannel() != null ? form.getChannel().name() : "");

        if (RequestContextUtils.getFlashMapManager(request) != null) {
            RequestContextUtils.getFlashMapManager(request).saveOutputFlashMap(flashMap, request, response);
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }
}
