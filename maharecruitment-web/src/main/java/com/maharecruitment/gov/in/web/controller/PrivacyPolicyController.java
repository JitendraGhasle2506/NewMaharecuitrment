package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public privacy policy of the MahaIT Face Attendance mobile application. It needs no login
 * (see SecurityConfig), is linked from the sign-in page and can be used as the app-store policy URL.
 */
@Controller
public class PrivacyPolicyController {

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }
}
