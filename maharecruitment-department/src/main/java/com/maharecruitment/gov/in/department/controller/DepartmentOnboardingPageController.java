package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.department.service.DepartmentOnboardingPageService;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;

@Controller
@RequestMapping("/department/onboarded")
public class DepartmentOnboardingPageController {

    private final DepartmentOnboardingPageService departmentOnboardingPageService;

    public DepartmentOnboardingPageController(DepartmentOnboardingPageService departmentOnboardingPageService) {
        this.departmentOnboardingPageService = departmentOnboardingPageService;
    }

    @GetMapping
    public String onboardedEmployeesPage(Principal principal, Model model) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("onboardedEmployees", departmentOnboardingPageService.getOnboardedEmployees(actorEmail));
        model.addAttribute("currentStatus", "ACTIVE");
        model.addAttribute("pageTitle", "Onboarded Employees");
        model.addAttribute("pageSubtitle", "Department-wise active onboarded employees.");
        return "department/department-onboarded-list";
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
