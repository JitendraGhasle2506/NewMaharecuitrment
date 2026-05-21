package com.maharecruitment.gov.in.web.controller.employee;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.web.service.employee.ManagerTaskService;

@Controller
@RequestMapping("/manager/tasks")
@PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_HOD')")
public class ManagerTaskController {

    private final ManagerTaskService managerTaskService;

    public ManagerTaskController(ManagerTaskService managerTaskService) {
        this.managerTaskService = managerTaskService;
    }

    @GetMapping("/approvals")
    public String viewApprovals(Model model, Principal principal) {
        String loginEmail = principal != null ? principal.getName() : null;
        
        model.addAttribute("pendingTasks", managerTaskService.getPendingTasksForManager(loginEmail));
        model.addAttribute("pageHeading", "Team Task Approvals");
        model.addAttribute("pageSubtitle", "Review and approve timesheets submitted by your direct reports.");
        
        return "employee/task-approvals";
    }

    @PostMapping("/approve")
    public String approveTask(@RequestParam("taskId") Long taskId, Principal principal, RedirectAttributes redirectAttributes) {
        String loginEmail = principal != null ? principal.getName() : null;
        try {
            managerTaskService.approveTask(taskId, loginEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Task approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving task.");
        }
        return "redirect:/manager/tasks/approvals";
    }

    @PostMapping("/reject")
    public String rejectTask(@RequestParam("taskId") Long taskId, @RequestParam("remarks") String remarks, Principal principal, RedirectAttributes redirectAttributes) {
        String loginEmail = principal != null ? principal.getName() : null;
        try {
            managerTaskService.rejectTask(taskId, remarks, loginEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Task rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting task.");
        }
        return "redirect:/manager/tasks/approvals";
    }
}
