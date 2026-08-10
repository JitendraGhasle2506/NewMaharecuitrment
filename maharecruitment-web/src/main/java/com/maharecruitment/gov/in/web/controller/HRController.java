package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String todayAttendance(
            @RequestParam(defaultValue = "CELL") String groupBy,
            Model model) {
        try {
            model.addAttribute("attendance", hrDashboardService.getTodayAttendance(groupBy));
            return "hr/hr_attendance_today";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/hr/attendance-today/details")
    public String todayAttendanceDetails(
            @RequestParam String category,
            @RequestParam(required = false) Long cellId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {
        try {
            model.addAttribute(
                    "attendanceDetail",
                    hrDashboardService.getTodayAttendanceDetails(
                            category,
                            cellId,
                            designationId,
                            departmentId,
                            page,
                            size));
            return "hr/hr_attendance_details";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/hr/wing-reports")
    public String wingReports(Model model) {
        model.addAttribute("wingDirectory", hrDashboardService.getWingReports());
        return "hr/hr_wing_reports";
    }

    @GetMapping("/hr/wing-report/{wingId}")
    public String wingReportDetail(@PathVariable Long wingId, Model model) {
        HRWingReportView wing = hrDashboardService.getWingReport(wingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wing report not found"));

        model.addAttribute("wing", wing);
        return "hr/wing_report_detail";
    }
}
