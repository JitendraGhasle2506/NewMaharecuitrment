package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.HrExternalInterviewService;
import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelUserOptionView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/hr/external-interviews")
@PreAuthorize("hasAuthority('ROLE_HR')")
@RequiredArgsConstructor
public class HrExternalInterviewController {

    private final HrExternalInterviewService hrExternalInterviewService;

    @GetMapping
    public String showExternalInterviews(Model model) {
        log.info("HR user accessed external interviews list");
        List<HrExternalInterviewCandidateView> candidates = hrExternalInterviewService.getExternalInterviewsForHr();
        model.addAttribute("candidates", candidates);
        
        return "hr/external-interviews-list";
    }

    @GetMapping("/{recruitmentInterviewDetailId}")
    public String interviewDetail(@PathVariable Long recruitmentInterviewDetailId, Model model, RedirectAttributes redirectAttributes) {
        try {
            HrExternalInterviewWorkflowDetailView detail = hrExternalInterviewService.getInterviewWorkflowDetail(recruitmentInterviewDetailId);
            List<InternalVacancyLevelTwoPanelUserOptionView> eligiblePanelUsers = hrExternalInterviewService.getEligiblePanelUsers();
            List<String> designations = eligiblePanelUsers.stream()
                    .map(InternalVacancyLevelTwoPanelUserOptionView::getDesignationName)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
            
            model.addAttribute("detail", detail);
            model.addAttribute("eligiblePanelUsers", eligiblePanelUsers);
            model.addAttribute("designations", designations);
            return "hr/external-interview-assign-panel";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/external-interviews";
        }
    }

    @PostMapping("/{recruitmentInterviewDetailId}/assign-panel")
    public String assignPanel(
            @PathVariable Long recruitmentInterviewDetailId,
            @RequestParam(name = "selectedUserIds", required = false) List<Long> selectedUserIds,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            hrExternalInterviewService.assignInterviewPanel(recruitmentInterviewDetailId, selectedUserIds, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Interview panel assigned successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error assigning panel: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while assigning the panel.");
        }

        return "redirect:/hr/external-interviews/" + recruitmentInterviewDetailId;
    }

    @PostMapping("/{recruitmentInterviewDetailId}/final-decision")
    public String submitFinalDecision(
            @PathVariable Long recruitmentInterviewDetailId,
            @RequestParam(name = "decisionRemarks", required = false) String decisionRemarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            hrExternalInterviewService.submitHrFinalDecision(
                    recruitmentInterviewDetailId,
                    decisionRemarks,
                    principal.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Final decision has been computed and recorded based on panel assessment scores.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error submitting final decision for interviewDetailId={}", recruitmentInterviewDetailId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while submitting the final decision.");
        }

        return "redirect:/hr/external-interviews/" + recruitmentInterviewDetailId;
    }

}
