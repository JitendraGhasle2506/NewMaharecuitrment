package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/agency/profile")
public class AgencyProfilePageController {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    @GetMapping
    public String profile(HttpSession session, Principal principal, Model model) {
        SessionUserDTO sessionUser = extractSessionUser(session);
        String fallbackUser = principal != null && principal.getName() != null
                ? principal.getName().trim()
                : "Agency User";

        model.addAttribute("profileName",
                sessionUser != null && sessionUser.name() != null ? sessionUser.name() : fallbackUser);
        model.addAttribute("profileEmail",
                sessionUser != null && sessionUser.email() != null ? sessionUser.email() : fallbackUser);
        model.addAttribute("profileMobile",
                sessionUser != null && sessionUser.mobileNo() != null && !sessionUser.mobileNo().isBlank()
                        ? sessionUser.mobileNo()
                        : "-");
        model.addAttribute("profileRoles", sessionUser != null && sessionUser.roles() != null
                ? sessionUser.roles()
                : List.of("ROLE_AGENCY"));
        return "agency/profile";
    }

    private SessionUserDTO extractSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object sessionUser = session.getAttribute(SESSION_USER_KEY);
        if (sessionUser instanceof SessionUserDTO dto) {
            return dto;
        }
        return null;
    }
}
