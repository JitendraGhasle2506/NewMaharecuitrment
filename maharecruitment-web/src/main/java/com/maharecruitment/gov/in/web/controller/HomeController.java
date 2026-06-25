package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.util.ContextPathUrlResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final OtpVerificationProperties otpVerificationProperties;
    private final NotificationChannelProperties notificationChannelProperties;
    private final ContextPathUrlResolver contextPathUrlResolver;

    public HomeController(
            OtpVerificationProperties otpVerificationProperties,
            NotificationChannelProperties notificationChannelProperties,
            ContextPathUrlResolver contextPathUrlResolver) {
        this.otpVerificationProperties = otpVerificationProperties;
        this.notificationChannelProperties = notificationChannelProperties;
        this.contextPathUrlResolver = contextPathUrlResolver;
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
    public String loginPage(Model model) {
        boolean otpEmailEnabled = notificationChannelProperties.isEmailEnabled();
        boolean otpSmsEnabled = notificationChannelProperties.isSmsEnabled();
        model.addAttribute("otpExpirySeconds", otpVerificationProperties.getExpirySeconds());
        model.addAttribute("otpResendCooldownSeconds", otpVerificationProperties.getResendCooldownSeconds());
        model.addAttribute("otpEmailEnabled", otpEmailEnabled);
        model.addAttribute("otpSmsEnabled", otpSmsEnabled);
        model.addAttribute("otpLoginEnabled", otpEmailEnabled || otpSmsEnabled);
        return "login";
    }
}
