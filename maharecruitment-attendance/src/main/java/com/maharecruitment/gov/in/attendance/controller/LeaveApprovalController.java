package com.maharecruitment.gov.in.attendance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.attendance.service.TourApplicationService;
import com.maharecruitment.gov.in.attendance.dto.LeaveApplicationHODDTO;
import com.maharecruitment.gov.in.attendance.dto.TourApplicationHODDTO;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/hod1")
public class LeaveApprovalController {
    private static final int HISTORY_PAGE_SIZE = 10;

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private TourApplicationService tourApplicationService;

    @GetMapping("/leaveApprovals")
    public String showLeaveApprovals(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
            @RequestParam(defaultValue = "leave") String activeTab,
            @RequestParam(defaultValue = "0") int leavePage,
            @RequestParam(defaultValue = "0") int tourPage,
            Model model, HttpSession session) {
        
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) {
            return "redirect:/login";
        }

        String selectedTab = normalizeTab(activeTab);
        List<LeaveApplicationHODDTO> pendingLeaves = List.of();
        List<TourApplicationHODDTO> pendingTours = List.of();
        Page<LeaveApplicationHODDTO> leaveHistory = Page.empty();
        Page<TourApplicationHODDTO> tourHistory = Page.empty();

        if ("leave".equals(selectedTab)) {
            pendingLeaves = leaveApplicationService.getPendingLeavesForHOD(user.id(), query, searchDate);
        } else if ("tour".equals(selectedTab)) {
            pendingTours = tourApplicationService.getPendingToursForHOD(user.id(), query, searchDate);
        } else {
            leaveHistory = leaveApplicationService.getProcessedLeavesForHOD(
                    user.id(), query, searchDate, PageRequest.of(Math.max(leavePage, 0), HISTORY_PAGE_SIZE));
            tourHistory = tourApplicationService.getProcessedToursForHOD(
                    user.id(), query, searchDate, PageRequest.of(Math.max(tourPage, 0), HISTORY_PAGE_SIZE));
        }

        model.addAttribute("pendingLeaves", pendingLeaves);
        model.addAttribute("pendingTours", pendingTours);
        model.addAttribute("processedLeaves", leaveHistory.getContent());
        model.addAttribute("processedTours", tourHistory.getContent());
        model.addAttribute("leaveHistoryPage", leaveHistory);
        model.addAttribute("tourHistoryPage", tourHistory);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        model.addAttribute("searchDate", searchDate);
        model.addAttribute("activeTab", selectedTab);

        return "attendance/leave-approvals";
    }

    @PostMapping("/approveLeave")
    public String approveLeave(@RequestParam("leaveId") Long leaveId, 
                             @RequestParam("remarks") String remarks,
                             @RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "searchDate", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        leaveApplicationService.updateLeaveStatus(
                leaveId, "APPROVED", remarks, requireSessionUser(session).id());
        redirectAttributes.addFlashAttribute("success", "Leave request approved successfully.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        if (searchDate != null) redirectAttributes.addAttribute("searchDate", searchDate);
        redirectAttributes.addAttribute("activeTab", "leave");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/rejectLeave")
    public String rejectLeave(@RequestParam("leaveId") Long leaveId, 
                            @RequestParam("remarks") String remarks,
                            @RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "searchDate", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        leaveApplicationService.updateLeaveStatus(
                leaveId, "REJECTED", remarks, requireSessionUser(session).id());
        redirectAttributes.addFlashAttribute("error", "Leave request rejected.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        if (searchDate != null) redirectAttributes.addAttribute("searchDate", searchDate);
        redirectAttributes.addAttribute("activeTab", "leave");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/approveTour")
    public String approveTour(@RequestParam("tourId") Long tourId, 
                             @RequestParam("remarks") String remarks,
                             @RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "searchDate", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        tourApplicationService.updateTourStatus(
                tourId, "APPROVED", remarks, requireSessionUser(session).id());
        redirectAttributes.addFlashAttribute("success", "Tour request approved successfully.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        if (searchDate != null) redirectAttributes.addAttribute("searchDate", searchDate);
        redirectAttributes.addAttribute("activeTab", "tour");
        return "redirect:/hod1/leaveApprovals";
    }

    @PostMapping("/rejectTour")
    public String rejectTour(@RequestParam("tourId") Long tourId, 
                            @RequestParam("remarks") String remarks,
                            @RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "searchDate", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        tourApplicationService.updateTourStatus(
                tourId, "REJECTED", remarks, requireSessionUser(session).id());
        redirectAttributes.addFlashAttribute("error", "Tour request rejected.");
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        if (searchDate != null) redirectAttributes.addAttribute("searchDate", searchDate);
        redirectAttributes.addAttribute("activeTab", "tour");
        return "redirect:/hod1/leaveApprovals";
    }

    private SessionUserDTO requireSessionUser(HttpSession session) {
        SessionUserDTO user = (SessionUserDTO) session.getAttribute("SESSION_USER");
        if (user == null) {
            throw new IllegalArgumentException("Your session has expired. Please sign in again.");
        }
        return user;
    }

    private String normalizeTab(String activeTab) {
        return "tour".equalsIgnoreCase(activeTab) || "history".equalsIgnoreCase(activeTab)
                ? activeTab.toLowerCase()
                : "leave";
    }
}
