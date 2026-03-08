package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;

@Controller
public class HRController {

    private final HRDashboardService hrDashboardService;

    public HRController(HRDashboardService hrDashboardService) {
        this.hrDashboardService = hrDashboardService;
    }

    @GetMapping("/hr/dashboard")
    public String hrDashboard(Model model) {
        HRDashboardView dashboard = hrDashboardService.getDashboard();

        model.addAttribute("totalProjects", dashboard.totalProjects());
        model.addAttribute("onboardingThisMonth", dashboard.onboardingThisMonth());
        model.addAttribute("internalEmployees", dashboard.internalEmployees());
        model.addAttribute("externalEmployees", dashboard.externalEmployees());
        model.addAttribute("totalEmployees", dashboard.totalEmployees());
        model.addAttribute("pendingApprovals", dashboard.pendingApprovals());
        model.addAttribute("openPositions", dashboard.openPositions());
        model.addAttribute("attritionRate", dashboard.attritionRate());
        model.addAttribute("internalPercent", dashboard.internalPercent());
        model.addAttribute("externalPercent", dashboard.externalPercent());
        model.addAttribute("departmentOnboarding", dashboard.departmentOnboarding());
        model.addAttribute("projects", dashboard.projects());

        return "hr/hr_dashboard";
    }
}
