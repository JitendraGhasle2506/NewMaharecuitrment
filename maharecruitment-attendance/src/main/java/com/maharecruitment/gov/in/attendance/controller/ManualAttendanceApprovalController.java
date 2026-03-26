package com.maharecruitment.gov.in.attendance.controller;

import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hod1/manual-attendance")
public class ManualAttendanceApprovalController {

    @Autowired
    private AttendanceRegisterService attendanceService;

    @GetMapping
    public String viewApprovals(Model model, HttpSession session, @RequestParam(required = false) String roleType) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) {
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
            return "attendance/manual-attendance-approvals";
        }

        if (roleType == null) {
            roleType = (user.roles() != null && user.roles().contains("ROLE_HOD")) ? "HOD" : "MANAGER";
        }

        Long approverId = "HOD".equalsIgnoreCase(roleType) ? user.id() : user.employeeId();

        model.addAttribute("pendingSummaries", attendanceService.getPendingSummaries(approverId, roleType));
        model.addAttribute("currentRoleType", roleType);

        return "attendance/manual-attendance-approvals";
    }

    @GetMapping("/details")
    public String viewDetails(@RequestParam("userId") Long targetUserId, 
                             @RequestParam(required = false) String roleType,
                             Model model, HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) return "redirect:/login";

        if (roleType == null) {
            roleType = (user.roles() != null && user.roles().contains("ROLE_HOD")) ? "HOD" : "MANAGER";
        }

        Long approverId = "HOD".equalsIgnoreCase(roleType) ? user.id() : user.employeeId();
        
        model.addAttribute("pendingRequests", attendanceService.getPendingRequestsForEmployee(approverId, targetUserId, roleType));
        model.addAttribute("currentRoleType", roleType);
        model.addAttribute("targetUserId", targetUserId);

        return "attendance/manual-attendance-details";
    }

    @PostMapping("/action")
    public String takeAction(
            @RequestParam("requestId") Long requestId,
            @RequestParam("userId") Long targetUserId,
            @RequestParam("status") String status, 
            @RequestParam("comments") String comments,
            @RequestParam("roleType") String roleType,
            HttpSession session, 
            RedirectAttributes redirectAttrs) {

        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) {
            redirectAttrs.addFlashAttribute("errorMessage", "Session expired.");
            return "redirect:/login";
        }

        Long approverId = "HOD".equalsIgnoreCase(roleType) ? user.id() : user.employeeId();

        try {
            attendanceService.approveRejectManualAttendance(requestId, approverId, status, comments, roleType);
            redirectAttrs.addFlashAttribute("successMessage", "Request " + status.toLowerCase() + " successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Error processing request: " + e.getMessage());
        }

        return "redirect:/hod1/manual-attendance/details?userId=" + targetUserId + "&roleType=" + roleType;
    }
}
