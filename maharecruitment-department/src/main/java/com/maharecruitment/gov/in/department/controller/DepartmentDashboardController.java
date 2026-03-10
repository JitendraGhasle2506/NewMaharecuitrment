package com.maharecruitment.gov.in.department.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/department")
public class DepartmentDashboardController {

    private static final String SESSION_USER_KEY = "SESSION_USER";

    private final DepartmentDashboardService departmentDashboardService;

    public DepartmentDashboardController(DepartmentDashboardService departmentDashboardService) {
        this.departmentDashboardService = departmentDashboardService;
    }

    @GetMapping("/home")
    public String dashboard(Model model, HttpSession session) {
        SessionUserDTO sessionUser = extractSessionUser(session);

        DepartmentDashboardView dashboard = departmentDashboardService.getDashboard(
                sessionUser != null ? sessionUser.departmentId() : null,
                sessionUser != null ? sessionUser.name() : null);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("isDummyData", true);
        return "department/dashboard";
    }

    private SessionUserDTO extractSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object candidate = session.getAttribute(SESSION_USER_KEY);
        if (candidate instanceof SessionUserDTO dto) {
            return dto;
        }

        return null;
    }
}
