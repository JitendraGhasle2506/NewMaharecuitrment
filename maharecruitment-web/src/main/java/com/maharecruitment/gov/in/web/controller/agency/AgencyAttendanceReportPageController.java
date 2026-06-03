package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.agency.AgencyAttendanceReportPageService;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportPageView;

@Controller
@RequestMapping("/agency/attendance-report")
public class AgencyAttendanceReportPageController {

    private static final Logger log = LoggerFactory.getLogger(AgencyAttendanceReportPageController.class);

    private final AgencyAttendanceReportPageService attendanceReportPageService;

    public AgencyAttendanceReportPageController(AgencyAttendanceReportPageService attendanceReportPageService) {
        this.attendanceReportPageService = attendanceReportPageService;
    }

    @GetMapping
    public String attendanceReport(
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "employeeType", required = false) String employeeType,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "redirect:/login";
        }

        try {
            AgencyAttendanceReportPageView reportView = attendanceReportPageService.getAttendanceReport(
                    principal.getName(),
                    month,
                    year,
                    employeeType,
                    search);
            populateModel(model, reportView);
            return "agency/attendance-report";
        } catch (RecruitmentNotificationException ex) {
            log.warn("Unable to load agency attendance report. actorEmail={}, reason={}",
                    principal.getName(),
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/agency/dashboard";
        }
    }

    private void populateModel(Model model, AgencyAttendanceReportPageView reportView) {
        model.addAttribute("reportView", reportView);
        model.addAttribute("agencyName", reportView.agencyName());
        model.addAttribute("filter", reportView.filter());
        model.addAttribute("monthNames", reportView.monthNames());
        model.addAttribute("yearOptions", reportView.yearOptions());
        model.addAttribute("daysInMonth", reportView.daysInMonth());
        model.addAttribute("reportRows", reportView.rows());
        model.addAttribute("summary", reportView.summary());
    }
}
