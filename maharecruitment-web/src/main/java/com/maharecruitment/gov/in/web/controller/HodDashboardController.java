package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.service.dashboard.HodDashboardService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HodDashboardController {
    private final HodDashboardService dashboardService;

    @GetMapping("/hod1/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null || user.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired.");
        }
        model.addAttribute("employees", dashboardService.getEmployees(user.id()));
        return "role/hod_dashboard";
    }
}
