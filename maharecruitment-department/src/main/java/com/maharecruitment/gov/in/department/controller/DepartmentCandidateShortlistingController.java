package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentCandidateShortlistingService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;

@Controller
@RequestMapping("/department/candidate-shortlisting")
public class DepartmentCandidateShortlistingController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentCandidateShortlistingController.class);

    private final DepartmentCandidateShortlistingService shortlistingService;

    public DepartmentCandidateShortlistingController(DepartmentCandidateShortlistingService shortlistingService) {
        this.shortlistingService = shortlistingService;
    }

    @GetMapping("/projects")
    public String projectQueue(Model model, Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        model.addAttribute("projectQueue", shortlistingService.getProjectQueue(actorEmail));
        return "department/candidate-shortlisting-project-list";
    }

    @GetMapping("/projects/{recruitmentNotificationId}/candidates")
    public String candidateList(
            @PathVariable Long recruitmentNotificationId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            DepartmentShortlistingDetailView detailView = shortlistingService.getShortlistingDetail(
                    recruitmentNotificationId,
                    actorEmail);
            model.addAttribute("shortlistingDetail", detailView);
            return "department/candidate-shortlisting-candidate-list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load department shortlisting detail. notificationId={}, reason={}",
                    recruitmentNotificationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/candidate-shortlisting/projects";
        } catch (RuntimeException ex) {
            log.error("Unexpected error while loading department shortlisting detail. notificationId={}",
                    recruitmentNotificationId,
                    ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load submitted candidates right now.");
            return "redirect:/department/candidate-shortlisting/projects";
        }
    }

    @PostMapping("/projects/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/review")
    public String reviewCandidate(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            @RequestParam("decision") DepartmentCandidateReviewDecision reviewDecision,
            @RequestParam(name = "remarks", required = false) String reviewRemarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            shortlistingService.reviewCandidate(
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    reviewRemarks,
                    actorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate review decision saved successfully.");
        } catch (DepartmentApplicationException ex) {
            log.warn(
                    "Unable to apply department candidate review. notificationId={}, candidateId={}, decision={}, reason={}",
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected error while applying department candidate review. notificationId={}, candidateId={}, decision={}",
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save candidate decision right now.");
        }

        return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId + "/candidates";
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
