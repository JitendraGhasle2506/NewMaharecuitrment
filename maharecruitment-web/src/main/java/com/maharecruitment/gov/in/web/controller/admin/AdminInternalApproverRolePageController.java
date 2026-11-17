package com.maharecruitment.gov.in.web.controller.admin;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService;
import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService.ChangeResult;
import com.maharecruitment.gov.in.attendance.service.InternalApproverRoleService.RoleOption;
import com.maharecruitment.gov.in.attendance.service.InternalEmployeeApprovalPolicy;

/**
 * Internal Approver Roles: who may decide INTERNAL employees' leave/tour requests.
 * Open to ADMIN and HR (see SecurityConfig).
 */
@Controller
@RequestMapping("/admin/internal-approver-roles")
public class AdminInternalApproverRolePageController {

    private static final Logger log = LoggerFactory.getLogger(AdminInternalApproverRolePageController.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final InternalApproverRoleService approverRoleService;

    public AdminInternalApproverRolePageController(InternalApproverRoleService approverRoleService) {
        this.approverRoleService = approverRoleService;
    }

    @GetMapping
    public String view(Authentication authentication, Model model) {
        List<RoleOption> roleOptions = approverRoleService.getRoleOptions();
        List<String> selectedRoles = roleOptions.stream().filter(RoleOption::selected).map(RoleOption::roleName).toList();
        model.addAttribute("roleOptions", roleOptions);
        model.addAttribute("selectedRoleCount", selectedRoles.size());
        model.addAttribute("approverLabel", InternalEmployeeApprovalPolicy.approverRoleLabel(selectedRoles));
        model.addAttribute("adminUser", hasAuthority(authentication, ROLE_ADMIN));
        return "admin/internal-approver-roles/list";
    }

    /** HR also opens this screen; the user-administration quick links are shown to admins only. */
    private static boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    @PostMapping
    public String save(
            @RequestParam(name = "roleIds", required = false) List<Long> roleIds,
            @RequestParam(name = "remarks", required = false) String remarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            ChangeResult result = approverRoleService.updateApproverRoles(
                    roleIds, remarks, principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", successMessage(result));
        } catch (IllegalArgumentException ex) {
            log.warn("Internal approver roles not saved: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("remarks", remarks);
        }
        return "redirect:/admin/internal-approver-roles";
    }

    private static String successMessage(ChangeResult result) {
        if (!result.changed()) {
            return "No changes to save. The approver roles are the same as before.";
        }
        StringBuilder message = new StringBuilder("Approver roles saved.");
        if (!result.added().isEmpty()) {
            message.append(" Added: ").append(String.join(", ", result.added())).append('.');
        }
        if (!result.removed().isEmpty()) {
            message.append(" Removed: ").append(String.join(", ", result.removed())).append('.');
        }
        return message.append(" The change applies to the next approval action and is recorded in the log.")
                .toString();
    }
}
