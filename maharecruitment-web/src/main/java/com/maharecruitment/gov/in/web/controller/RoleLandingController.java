package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.maharecruitment.gov.in.web.service.dashboard.AgencyDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.MDDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.RoleLandingService;
import com.maharecruitment.gov.in.web.service.dashboard.model.MDDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.RoleDashboardView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RoleLandingController {

    private final RoleLandingService roleLandingService;
    private final AgencyDashboardService agencyDashboardService;
    private final MDDashboardService mdDashboardService;

    public RoleLandingController(
            RoleLandingService roleLandingService,
            AgencyDashboardService agencyDashboardService,
            MDDashboardService mdDashboardService) {
        this.roleLandingService = roleLandingService;
        this.agencyDashboardService = agencyDashboardService;
        this.mdDashboardService = mdDashboardService;
    }

    @GetMapping({
            "/admin/dashboard",
            "/user/dashboard",
            "/agency/dashboard",
            "/stm/dashboard",
            "/pm/dashboard",
            "/hod1/dashboard",
            "/hod2/dashboard",
            "/coo/dashboard",
            "/md/dashboard",
            "/pension",
            "/hrms",
            "/payroll"
    })
    public String roleLanding(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if ("/agency/dashboard".equals(path)) {
            com.maharecruitment.gov.in.web.service.dashboard.model.AgencyDashboardView agencyDashboard = agencyDashboardService.getDashboard();
            model.addAttribute("agencyName", agencyDashboard.agencyName());
            model.addAttribute("totalOpenings", agencyDashboard.totalOpenings());
            model.addAttribute("candidatesSubmitted", agencyDashboard.candidatesSubmitted());
            model.addAttribute("interviewsScheduled", agencyDashboard.interviewsScheduled());
            model.addAttribute("onboardedEmployees", agencyDashboard.onboardedEmployees());
            model.addAttribute("status", agencyDashboard.status());
            model.addAttribute("recentNotifications", agencyDashboard.recentNotifications());
            return "agency/agency_dashboard";
        }

        if ("/md/dashboard".equals(path)) {
            MDDashboardView dashboard = mdDashboardService.getDashboard();
            model.addAttribute("dashboard", dashboard);
            return "md/md_dashboard";
        }

        RoleDashboardView dashboard = roleLandingService.getDashboardByPath(path);
        model.addAttribute("title", dashboard.title());
        model.addAttribute("subtitle", dashboard.subtitle());
        model.addAttribute("activeProjects", dashboard.activeProjects());
        model.addAttribute("newOnboarding", dashboard.newOnboarding());
        model.addAttribute("internalCount", dashboard.internalCount());
        model.addAttribute("externalCount", dashboard.externalCount());
        model.addAttribute("pendingApprovals", dashboard.pendingApprovals());
        model.addAttribute("alerts", dashboard.alerts());
        model.addAttribute("tasks", dashboard.tasks());

        return "role/role_dashboard";
    }
}
