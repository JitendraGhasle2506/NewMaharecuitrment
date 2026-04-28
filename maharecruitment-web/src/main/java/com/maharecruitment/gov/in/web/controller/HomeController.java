package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final OtpVerificationProperties otpVerificationProperties;

    public HomeController(OtpVerificationProperties otpVerificationProperties) {
        this.otpVerificationProperties = otpVerificationProperties;
    }

    @GetMapping("/home")
    public String home(HttpSession session) {
        if (session != null) {
            Object homepageUrl = session.getAttribute("homepageUrl");
            if (homepageUrl instanceof String targetUrl
                    && !targetUrl.isBlank()
                    && !"/home".equals(targetUrl)) {
                return "redirect:" + targetUrl;
            }
        }

        return "redirect:/common";
    }

    @GetMapping({ "/", "/index", "/login" })
    public String loginPage(Model model) {
        model.addAttribute("otpExpirySeconds", otpVerificationProperties.getExpirySeconds());
        model.addAttribute("otpResendCooldownSeconds", otpVerificationProperties.getResendCooldownSeconds());
        return "login";
    }
}
