package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

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
    public String loginPage() {
        return "login";
    }
}
