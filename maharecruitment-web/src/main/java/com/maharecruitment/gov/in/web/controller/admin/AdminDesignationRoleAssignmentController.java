package com.maharecruitment.gov.in.web.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.service.DesignationRoleAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentResult;

@Controller
@RequestMapping("/admin/designation-role-assignments")
public class AdminDesignationRoleAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(AdminDesignationRoleAssignmentController.class);

    private final DesignationRoleAssignmentService assignmentService;

    public AdminDesignationRoleAssignmentController(DesignationRoleAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        var designationOptions = assignmentService.getAssignments(null);
        var assignments = normalizedSearch == null
                ? designationOptions
                : assignmentService.getAssignments(normalizedSearch);
        model.addAttribute("assignments", assignments);
        model.addAttribute("designationOptions", designationOptions);
        model.addAttribute("availableRoleNames", assignmentService.getAssignableRoleNames());
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("totalEmployees", assignments.stream()
                .mapToLong(assignment -> assignment.activeEmployeeCount())
                .sum());
        model.addAttribute("pendingUsers", assignments.stream()
                .mapToLong(assignment -> assignment.pendingUserCount())
                .sum());
        model.addAttribute("unconfiguredDesignations", assignments.stream()
                .filter(assignment -> !assignment.configuredRoleAvailable())
                .count());
        return "admin/designation-role-assignments/list";
    }

    @PostMapping("/assign")
    public String assignSelectedRole(
            @RequestParam Long designationId,
            @RequestParam String roleName,
            @RequestParam(name = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        return configureAndAssign(designationId, roleName, search, redirectAttributes);
    }

    @PostMapping("/{designationId}/assign")
    public String configureAndAssign(
            @PathVariable Long designationId,
            @RequestParam String roleName,
            @RequestParam(name = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            DesignationRoleAssignmentResult result = assignmentService
                    .configureAndAssign(designationId, roleName);
            redirectAttributes.addFlashAttribute("successMessage", resultMessage(result));
        } catch (RuntimeException ex) {
            log.error("Designation role assignment failed for designationId={}", designationId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addSearchRedirectAttribute(search, redirectAttributes);
        return "redirect:/admin/designation-role-assignments";
    }

    @PostMapping("/assign-all")
    public String assignAll(
            @RequestParam(name = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            DesignationRoleAssignmentResult result = assignmentService.assignAllConfiguredRoles();
            redirectAttributes.addFlashAttribute("successMessage", resultMessage(result));
        } catch (RuntimeException ex) {
            log.error("Bulk designation role assignment failed", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        addSearchRedirectAttribute(search, redirectAttributes);
        return "redirect:/admin/designation-role-assignments";
    }

    private String resultMessage(DesignationRoleAssignmentResult result) {
        String message = "Roles assigned to " + result.assignedUsers()
                + " user(s); " + result.alreadyAssignedUsers() + " already assigned";
        if (result.missingUserAccounts() > 0) {
            message += "; " + result.missingUserAccounts() + " employee(s) have no linked user account";
        }
        if (result.inactiveUserAccounts() > 0) {
            message += "; " + result.inactiveUserAccounts() + " user account(s) are inactive";
        }
        if (result.skippedDesignations() > 0) {
            message += "; " + result.skippedDesignations() + " designation(s) need a valid role";
        }
        return message + ".";
    }

    private void addSearchRedirectAttribute(String search, RedirectAttributes redirectAttributes) {
        if (StringUtils.hasText(search)) {
            redirectAttributes.addAttribute("search", search.trim());
        }
    }
}
