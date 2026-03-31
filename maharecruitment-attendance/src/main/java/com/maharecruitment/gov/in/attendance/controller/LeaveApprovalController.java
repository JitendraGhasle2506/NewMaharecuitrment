package com.maharecruitment.gov.in.attendance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/hod1")
public class LeaveApprovalController {

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private TourApplicationService tourApplicationService;

    @GetMapping("/leaveApprovals")
    public String showLeaveApprovals(
            @RequestParam(required = false) String query,
            Model model, HttpSession session) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("pendingLeaves", leaveApplicationService.getPendingLeavesForHOD(user.id(), query));
        model.addAttribute("pendingTours", tourApplicationService.getPendingToursForHOD(user.id(), query));
        
        model.addAttribute("processedLeaves", leaveApplicationService.getProcessedLeavesForHOD(user.id(), query));
        model.addAttribute("processedTours", tourApplicationService.getProcessedToursForHOD(user.id(), query));
        
        model.addAttribute("searchQuery", query);

        return "attendance/leave-approvals";
    }

    @PostMapping("/approveLeave")
    public String approveLeave(@RequestParam("leaveId") Long leaveId, 
                             @RequestParam("remarks") String remarks,
                             @RequestParam(value = "query", required = false) String query,
                             RedirectAttributes redirectAttributes) {
        leaveApplicationService.updateLeaveStatus(leaveId, "APPROVED", remarks);
        redirectAttributes.addFlashAttribute("success", "Leave request approved successfully.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        redirectAttributes.addAttribute("activeTab", "leave");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/rejectLeave")
    public String rejectLeave(@RequestParam("leaveId") Long leaveId, 
                            @RequestParam("remarks") String remarks,
                            @RequestParam(value = "query", required = false) String query,
                            RedirectAttributes redirectAttributes) {
        leaveApplicationService.updateLeaveStatus(leaveId, "REJECTED", remarks);
        redirectAttributes.addFlashAttribute("error", "Leave request rejected.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        redirectAttributes.addAttribute("activeTab", "leave");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/approveTour")
    public String approveTour(@RequestParam("tourId") Long tourId, 
                             @RequestParam("remarks") String remarks,
                             @RequestParam(value = "query", required = false) String query,
                             RedirectAttributes redirectAttributes) {
        tourApplicationService.updateTourStatus(tourId, "APPROVED", remarks);
        redirectAttributes.addFlashAttribute("success", "Tour request approved successfully.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        redirectAttributes.addAttribute("activeTab", "tour");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/rejectTour")
    public String rejectTour(@RequestParam("tourId") Long tourId, 
                            @RequestParam("remarks") String remarks,
                            @RequestParam(value = "query", required = false) String query,
                            RedirectAttributes redirectAttributes) {
        tourApplicationService.updateTourStatus(tourId, "REJECTED", remarks);
        redirectAttributes.addFlashAttribute("error", "Tour request rejected.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        redirectAttributes.addAttribute("activeTab", "tour");
        return "redirect:/hod1/leaveApprovals";
    }
}
