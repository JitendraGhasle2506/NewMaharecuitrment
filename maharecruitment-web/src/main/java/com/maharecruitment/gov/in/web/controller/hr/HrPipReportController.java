package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan.Status;
import com.maharecruitment.gov.in.web.service.pip.HrPipReportService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/hr/pip")
@PreAuthorize("hasAuthority('ROLE_HR')")
@RequiredArgsConstructor
public class HrPipReportController {
    private final HrPipReportService service;

    @GetMapping
    public String overview(@RequestParam(required = false) Status status, @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        model.addAttribute("report", service.overview(principal.getName(), status, q, page));
        model.addAttribute("currentStatus", status == null ? "" : status.name());
        model.addAttribute("q", q);
        return "hr/pip-overview";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("detail", service.detail(principal.getName(), id));
        model.addAttribute("hrReadOnly", true);
        model.addAttribute("today", LocalDate.now(ZoneId.of("Asia/Kolkata")));
        return "employee/pip-detail";
    }
}
