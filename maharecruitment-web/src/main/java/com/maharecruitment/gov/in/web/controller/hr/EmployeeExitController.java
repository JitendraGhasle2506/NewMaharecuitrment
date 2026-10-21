package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeExitForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/employees/{employeeId}/exit")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeExitController {
    private final EmployeeExitService exitService;
    private final HROnboardingPageService onboarding;

    public EmployeeExitController(EmployeeExitService exitService, HROnboardingPageService onboarding) {
        this.exitService = exitService;
        this.onboarding = onboarding;
    }

    @GetMapping
    public String show(@PathVariable Long employeeId, Model model) {
        var form = new EmployeeExitForm();
        form.setLastWorkingDate(today());
        model.addAttribute("exitForm", form);
        return populate(employeeId, model);
    }

    @PostMapping
    public String exit(@PathVariable Long employeeId, @Valid @ModelAttribute("exitForm") EmployeeExitForm form,
            BindingResult errors, Principal principal, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return populate(employeeId, model);
        try {
            exitService.exitEmployee(employeeId, form.getLastWorkingDate(), form.getReason(), principal.getName());
        } catch (RecruitmentNotificationException ex) {
            errors.reject("exit.failed", ex.getMessage());
            return populate(employeeId, model);
        }
        redirect.addFlashAttribute("successMessage", "Employee exit recorded. System access has been disabled.");
        return "redirect:/hr/employees/resigned";
    }

    private String populate(Long employeeId, Model model) {
        model.addAttribute("employeeDetail", onboarding.loadEmployeeDetail(employeeId));
        model.addAttribute("today", today());
        return "hr/employee-exit";
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
}
