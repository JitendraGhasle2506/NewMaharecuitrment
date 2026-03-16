package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;

@Controller
@RequestMapping("/agency/selected-candidates")
public class AgencySelectedCandidatePageController {

    private final AgencyRecruitmentNotificationPageService pageService;

    public AgencySelectedCandidatePageController(AgencyRecruitmentNotificationPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String selectedCandidateProjects(Principal principal, Model model) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("selectedProjects", pageService.getSelectedCandidateProjects(actorEmail));
        return "agency/selected-candidate-project-list";
    }

    @GetMapping("/{recruitmentNotificationId}")
    public String selectedCandidatesByProject(
            @PathVariable Long recruitmentNotificationId,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("selectedCandidates", pageService.getSelectedCandidates(actorEmail, recruitmentNotificationId));
        model.addAttribute("selectedRecruitmentNotificationId", recruitmentNotificationId);
        return "agency/selected-candidate-list";
    }

    @PostMapping("/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/withdraw")
    public String withdrawSelectedCandidate(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            pageService.withdrawCandidate(actorEmail, recruitmentNotificationId, recruitmentInterviewDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate withdrawn successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agency/selected-candidates/" + recruitmentNotificationId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
