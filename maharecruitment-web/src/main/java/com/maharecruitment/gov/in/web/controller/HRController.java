package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRWingReportView;

@Controller
public class HRController {

    private final HRDashboardService hrDashboardService;

    public HRController(HRDashboardService hrDashboardService) {
        this.hrDashboardService = hrDashboardService;
    }

    @GetMapping("/hr/dashboard")
    public String hrDashboard(Model model) {
        model.addAttribute("dashboard", hrDashboardService.getDashboard());
        return "hr/hr_dashboard";
    }

    @GetMapping("/hr/attendance-today")
    public String todayAttendance(Model model) {
        model.addAttribute("attendance", hrDashboardService.getTodayAttendance());
        return "hr/hr_attendance_today";
    }

    @GetMapping("/hr/wing-report/{wingId}")
    public String wingReportDetail(@PathVariable Long wingId, Model model) {
        HRWingReportView wing = hrDashboardService.getWingReport(wingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wing report not found"));

        model.addAttribute("wing", wing);
        return "hr/wing_report_detail";
    }
}
