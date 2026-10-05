package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.util.ContextPathUrlResolver;
import com.maharecruitment.gov.in.web.service.security.LoginCaptchaService;
import com.maharecruitment.gov.in.web.service.security.LoginCaptchaService.LoginCaptchaChallenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final OtpVerificationProperties otpVerificationProperties;
    private final NotificationChannelProperties notificationChannelProperties;
    private final ContextPathUrlResolver contextPathUrlResolver;
    private final LoginCaptchaService loginCaptchaService;

    public HomeController(
            OtpVerificationProperties otpVerificationProperties,
            NotificationChannelProperties notificationChannelProperties,
            ContextPathUrlResolver contextPathUrlResolver,
            LoginCaptchaService loginCaptchaService) {
        this.otpVerificationProperties = otpVerificationProperties;
        this.notificationChannelProperties = notificationChannelProperties;
        this.contextPathUrlResolver = contextPathUrlResolver;
        this.loginCaptchaService = loginCaptchaService;
    }

    @GetMapping("/home")
    public String home(HttpServletRequest request, HttpSession session) {
        if (session != null) {
            Object homepageUrl = session.getAttribute("homepageUrl");
            if (homepageUrl instanceof String targetUrl
                    && !targetUrl.isBlank()
                    && !"/home".equals(targetUrl)) {
                String redirectPath = contextPathUrlResolver.toRedirectPath(
                        request.getContextPath(),
                        targetUrl,
                        "/common");
                if (!"/home".equals(redirectPath)) {
                    return "redirect:" + redirectPath;
                }
            }
        }

        return "redirect:/common";
    }

    @GetMapping({ "/", "/index", "/login" })
    public String loginPage(Model model, HttpSession session) {
        boolean otpEmailEnabled = notificationChannelProperties.isEmailEnabled();
        boolean otpSmsEnabled = notificationChannelProperties.isSmsEnabled();
        model.addAttribute("otpExpirySeconds", otpVerificationProperties.getExpirySeconds());
        model.addAttribute("otpResendCooldownSeconds", otpVerificationProperties.getResendCooldownSeconds());
        model.addAttribute("otpEmailEnabled", otpEmailEnabled);
        model.addAttribute("otpSmsEnabled", otpSmsEnabled);
        model.addAttribute("otpBothEnabled", otpEmailEnabled && otpSmsEnabled);
        model.addAttribute("otpLoginEnabled", otpEmailEnabled || otpSmsEnabled);
        model.addAttribute("loginCaptcha", loginCaptchaService.createChallenge(session));
        return "login";
    }

    @GetMapping("/login/captcha")
    @ResponseBody
    public ResponseEntity<LoginCaptchaChallenge> loginCaptcha(
            @RequestParam(required = false) String previousCaptchaId,
            HttpSession session) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(loginCaptchaService.replaceChallenge(session, previousCaptchaId));
    }

    @GetMapping(value = "/login/captcha/{captchaId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> loginCaptchaImage(
            @PathVariable String captchaId,
            HttpSession session) {
        return loginCaptchaService.getChallengeImage(session, captchaId)
                .map(image -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.IMAGE_PNG)
                        .body(image))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
