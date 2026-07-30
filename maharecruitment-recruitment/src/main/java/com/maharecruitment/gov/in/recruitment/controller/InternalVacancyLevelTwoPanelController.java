package com.maharecruitment.gov.in.recruitment.controller;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.dto.internal.InternalVacancyLevelTwoFeedbackForm;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyLevelTwoPanelReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoFeedbackSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelWorkflowDetailView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/panel/internal-vacancies/level-two")
@PreAuthorize("hasAnyAuthority('ROLE_COO', 'ROLE_HOD', 'ROLE_STM')")
public class InternalVacancyLevelTwoPanelController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final InternalVacancyLevelTwoPanelReviewService panelReviewService;

    public InternalVacancyLevelTwoPanelController(InternalVacancyLevelTwoPanelReviewService panelReviewService) {
        this.panelReviewService = panelReviewService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = resolvePageSize(size);
        String normalizedSearch = normalizeSearch(search);

        Page<InternalVacancyLevelTwoPanelCandidateView> candidatePage = loadCandidatePage(
                resolveActorEmail(principal),
                normalizedSearch,
                resolvedPage,
                resolvedSize);
        if (candidatePage.getTotalPages() > 0 && resolvedPage >= candidatePage.getTotalPages()) {
            candidatePage = loadCandidatePage(
                    resolveActorEmail(principal),
                    normalizedSearch,
                    candidatePage.getTotalPages() - 1,
                    resolvedSize);
        }

        model.addAttribute("candidatePage", candidatePage);
        model.addAttribute("levelTwoCandidates", candidatePage.getContent());
        model.addAttribute("searchTerm", normalizedSearch == null ? "" : normalizedSearch);
        model.addAttribute("pageSize", candidatePage.getSize());
        return "panel/internal-vacancy-level-two-list";
    }

    @GetMapping("/{recruitmentInterviewDetailId}")
    public String detail(
            @PathVariable Long recruitmentInterviewDetailId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            InternalVacancyLevelTwoPanelWorkflowDetailView detail = panelReviewService.getWorkflowDetail(
                    resolveActorEmail(principal),
                    recruitmentInterviewDetailId);
            populateDetailModel(model, detail);
            return "panel/internal-vacancy-level-two-detail";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/panel/internal-vacancies/level-two";
        }
    }

    @PostMapping("/{recruitmentInterviewDetailId}/feedback")
    public String submitFeedback(
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("feedbackForm") InternalVacancyLevelTwoFeedbackForm feedbackForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        InternalVacancyLevelTwoPanelWorkflowDetailView detail;
        try {
            detail = panelReviewService.getWorkflowDetail(resolveActorEmail(principal), recruitmentInterviewDetailId);
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/panel/internal-vacancies/level-two";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("detail", detail);
            model.addAttribute("feedbackForm", feedbackForm);
            return "panel/internal-vacancy-level-two-detail";
        }

        try {
            panelReviewService.submitFeedback(
                    resolveActorEmail(principal),
                    recruitmentInterviewDetailId,
                    toSubmissionInput(feedbackForm));
            redirectAttributes.addFlashAttribute("successMessage", "Round L2 feedback submitted successfully.");
            return "redirect:/panel/internal-vacancies/level-two/" + recruitmentInterviewDetailId;
        } catch (RecruitmentNotificationException ex) {
            model.addAttribute("detail", detail);
            model.addAttribute("feedbackForm", feedbackForm);
            model.addAttribute("errorMessage", ex.getMessage());
            return "panel/internal-vacancy-level-two-detail";
        }
    }

    private void populateDetailModel(Model model, InternalVacancyLevelTwoPanelWorkflowDetailView detail) {
        model.addAttribute("detail", detail);
        if (!model.containsAttribute("feedbackForm")) {
            model.addAttribute("feedbackForm", buildFeedbackForm(detail));
        }
    }

    private InternalVacancyLevelTwoFeedbackForm buildFeedbackForm(InternalVacancyLevelTwoPanelWorkflowDetailView detail) {
        InternalVacancyLevelTwoFeedbackForm form = new InternalVacancyLevelTwoFeedbackForm();
        if (detail.getMyFeedback() == null) {
            return form;
        }
        form.setCommunicationSkillMarks(detail.getMyFeedback().getCommunicationSkillMarks());
        form.setTechnicalSkillMarks(detail.getMyFeedback().getTechnicalSkillMarks());
        form.setLeadershipQualityMarks(detail.getMyFeedback().getLeadershipQualityMarks());
        form.setRelevantExperienceMarks(detail.getMyFeedback().getRelevantExperienceMarks());
        form.setInterviewerGrade(detail.getMyFeedback().getInterviewerGrade());
        form.setRecommendationStatus(detail.getMyFeedback().getRecommendationStatus());
        form.setAssessmentRemarks(detail.getMyFeedback().getAssessmentRemarks());
        form.setFinalRemarks(detail.getMyFeedback().getFinalRemarks());
        return form;
    }

    private InternalVacancyLevelTwoFeedbackSubmissionInput toSubmissionInput(
            InternalVacancyLevelTwoFeedbackForm form) {
        return InternalVacancyLevelTwoFeedbackSubmissionInput.builder()
                .communicationSkillMarks(form.getCommunicationSkillMarks())
                .technicalSkillMarks(form.getTechnicalSkillMarks())
                .leadershipQualityMarks(form.getLeadershipQualityMarks())
                .relevantExperienceMarks(form.getRelevantExperienceMarks())
                .interviewerGrade(form.getInterviewerGrade())
                .recommendationStatus(form.getRecommendationStatus())
                .assessmentRemarks(form.getAssessmentRemarks())
                .finalRemarks(form.getFinalRemarks())
                .build();
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private Page<InternalVacancyLevelTwoPanelCandidateView> loadCandidatePage(
            String actorEmail,
            String search,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                resolvePageSize(size),
                Sort.by(Sort.Direction.DESC, "interviewDateTime")
                        .and(Sort.by(Sort.Direction.DESC, "scheduledAt")));
        return panelReviewService.getAssignedCandidatePage(actorEmail, search, pageable);
    }

    private int resolvePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
