package com.maharecruitment.gov.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

@Controller
public class HRController {

    private final HRDashboardService hrDashboardService;

    public HRController(HRDashboardService hrDashboardService) {
        this.hrDashboardService = hrDashboardService;
    }

    @GetMapping("/hr/dashboard")
    public String hrDashboard(Model model) {
        HRDashboardView dashboard = hrDashboardService.getDashboard();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("totalProjects", dashboard.totalProjects());
        model.addAttribute("internalProjects", dashboard.internalProjects());
        model.addAttribute("externalProjects", dashboard.externalProjects());
        model.addAttribute("onboardingThisMonth", dashboard.onboardingThisMonth());
        model.addAttribute("internalEmployees", dashboard.internalEmployees());
        model.addAttribute("externalEmployees", dashboard.externalEmployees());
        model.addAttribute("totalEmployees", dashboard.totalEmployees());
        model.addAttribute("presentEmployees", dashboard.presentEmployees());
        model.addAttribute("absentEmployees", dashboard.absentEmployees());
        model.addAttribute("presentPercent", dashboard.presentPercent());
        model.addAttribute("checkedInEmployees", dashboard.attendanceSummary().checkedInEmployees());
        model.addAttribute("earlyCheckIns", dashboard.attendanceSummary().earlyCheckIns());
        model.addAttribute("standardCheckIns", dashboard.attendanceSummary().standardCheckIns());
        model.addAttribute("lateCheckIns", dashboard.attendanceSummary().lateCheckIns());
        model.addAttribute("pendingApprovals", dashboard.pendingApprovals());
        model.addAttribute("totalWings", dashboard.totalWings());
        model.addAttribute("totalCells", dashboard.totalCells());
        model.addAttribute("wingProjectCount", dashboard.wingProjectCount());
        model.addAttribute("wingEmployeeCount", dashboard.wingEmployeeCount());
        model.addAttribute("internalPercent", dashboard.internalPercent());
        model.addAttribute("externalPercent", dashboard.externalPercent());
        model.addAttribute("departmentOnboarding", dashboard.departmentOnboarding());
        model.addAttribute("internalProjectList", dashboard.internalProjectList());
        model.addAttribute("externalProjectList", dashboard.externalProjectList());
        model.addAttribute("wingReports", dashboard.wingReports());

        return "hr/hr_dashboard";
    }

    @GetMapping("/hr/wing-report/{wingId}")
    public String wingReportDetail(@PathVariable Long wingId, Model model) {
        HRWingReportView wing = hrDashboardService.getWingReport(wingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wing report not found"));

        model.addAttribute("wing", wing);
        return "hr/wing_report_detail";
    }
}
