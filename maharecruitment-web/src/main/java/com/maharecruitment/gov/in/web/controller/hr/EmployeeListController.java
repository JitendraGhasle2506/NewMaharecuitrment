package com.maharecruitment.gov.in.web.controller.hr;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;

@Controller
@RequestMapping("/hr/employees")
@PreAuthorize("hasRole('ROLE_HR')")
public class EmployeeListController {

    private final HROnboardingPageService hrOnboardingPageService;

    public EmployeeListController(HROnboardingPageService hrOnboardingPageService) {
        this.hrOnboardingPageService = hrOnboardingPageService;
    }

    @GetMapping
    public String employeeList(Model model) {
        model.addAttribute("employees", hrOnboardingPageService.getOnboardedEmployees());
        return "hr/employee-list";
    }
}
