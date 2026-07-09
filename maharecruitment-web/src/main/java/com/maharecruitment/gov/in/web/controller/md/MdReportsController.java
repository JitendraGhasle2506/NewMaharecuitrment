package com.maharecruitment.gov.in.web.controller.md;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.web.service.dashboard.MdWorkforceReportService;

@Controller
@RequestMapping("/md/reports")
public class MdReportsController {

    private final MdWorkforceReportService workforceReportService;

    public MdReportsController(MdWorkforceReportService workforceReportService) {
        this.workforceReportService = workforceReportService;
    }

    @GetMapping("/wing-cell-employees")
    public String wingCellEmployeeReport(Model model) {
        model.addAttribute("report", workforceReportService.getReport());
        return "md/wing-cell-employee-report-visual";
    }
}
