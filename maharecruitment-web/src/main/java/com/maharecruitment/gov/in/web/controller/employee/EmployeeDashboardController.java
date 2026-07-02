package com.maharecruitment.gov.in.web.controller.employee;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;

@Controller
@RequestMapping("/employee")
@PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
public class EmployeeDashboardController {

    private final EmployeeProfileService employeeProfileService;

    public EmployeeDashboardController(EmployeeProfileService employeeProfileService) {
        this.employeeProfileService = employeeProfileService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        try {
            String loginEmail = principal != null ? principal.getName() : null;
            model.addAttribute("employeeDetail", employeeProfileService.loadCurrentEmployeeProfile(loginEmail));
            model.addAttribute("pageHeading", "My Profile");
            model.addAttribute("pageSubtitle", "All details submitted by agency and confirmed by HR are available here.");
            return "employee/dashboard";
        } catch (RecruitmentNotificationException ex) {
            model.addAttribute("pageHeading", "My Profile");
            model.addAttribute("pageSubtitle", "Your confirmed employee profile is not available yet.");
            model.addAttribute("errorMessage", ex.getMessage());
            return "employee/profile-unavailable";
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/upload-photo")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> uploadPhoto(
            Principal principal,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String loginEmail = principal != null ? principal.getName() : null;
            employeeProfileService.updateEmployeePhoto(loginEmail, file);
            return org.springframework.http.ResponseEntity.ok().body("{\"message\": \"Photo uploaded successfully\"}");
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.badRequest().body("{\"error\": \"" + ex.getMessage() + "\"}");
        }
    }
}
