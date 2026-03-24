package com.maharecruitment.gov.in.recruitment.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.dto.internal.InternalInterviewAssessmentForm;
import com.maharecruitment.gov.in.recruitment.dto.internal.InternalInterviewAssessmentPanelMemberForm;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyInterviewAuthorityShortlistingService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyInterviewAuthorityWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentPanelMemberInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/interview-authority/internal-vacancies")
@PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_PM', 'ROLE_STM')")
public class InternalVacancyInterviewAuthorityController {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyInterviewAuthorityController.class);
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

    private final UserRepository userRepository;
    private final InternalVacancyInterviewAuthorityShortlistingService shortlistingService;
    private final InternalVacancyInterviewAuthorityWorkflowService workflowService;

    public InternalVacancyInterviewAuthorityController(
            UserRepository userRepository,
            InternalVacancyInterviewAuthorityShortlistingService shortlistingService,
            InternalVacancyInterviewAuthorityWorkflowService workflowService) {
        this.userRepository = userRepository;
        this.shortlistingService = shortlistingService;
        this.workflowService = workflowService;
    }

    @GetMapping
    public String requestList(Model model, Principal principal) {
        model.addAttribute("requestSummaries",
                shortlistingService.getAssignedRequestSummaries(resolveActorEmail(principal)));
        return "interview-authority/internal-vacancy-request-list";
    }

    @GetMapping("/request/{requestId}/candidates")
    public String candidateList(
            @PathVariable String requestId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            InternalVacancyCandidateListView candidateListView = shortlistingService
                    .getAssignedCandidatesByRequestId(resolveActorEmail(principal), requestId);
            model.addAttribute("candidateListView", candidateListView);
            return "interview-authority/internal-vacancy-candidate-list";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/interview-authority/internal-vacancies";
        }
    }

    @GetMapping("/request/{requestId}/candidates/{recruitmentInterviewDetailId}/feedback")
    public String feedbackPage(
            @PathVariable String requestId,
            @PathVariable Long recruitmentInterviewDetailId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            DepartmentInterviewWorkflowDetailView workflowDetail = workflowService.getInterviewWorkflowDetail(
                    resolveActorEmail(principal),
                    requestId,
                    recruitmentInterviewDetailId);
            model.addAttribute("workflowDetail", workflowDetail);
            if (!model.containsAttribute("assessmentForm")) {
                model.addAttribute(
                        "assessmentForm",
                        buildAssessmentForm(workflowDetail, principal != null ? principal.getName() : null));
            }
            return "interview-authority/internal-vacancy-feedback-form";
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/interview-authority/internal-vacancies/request/" + requestId + "/candidates";
        }
    }

    @PostMapping("/request/{requestId}/candidates/{recruitmentInterviewDetailId}/review")
    public String reviewCandidate(
            @PathVariable String requestId,
            @PathVariable Long recruitmentInterviewDetailId,
            @RequestParam("decision") DepartmentCandidateReviewDecision reviewDecision,
            @RequestParam(name = "remarks", required = false) String reviewRemarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            shortlistingService.reviewCandidate(
                    resolveActorEmail(principal),
                    requestId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    reviewRemarks);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate shortlist decision saved successfully.");
        } catch (RecruitmentNotificationException ex) {
            log.warn(
                    "Unable to apply interview-authority shortlist decision. requestId={}, candidateId={}, decision={}, reason={}",
                    requestId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected error while applying interview-authority shortlist decision. requestId={}, candidateId={}, decision={}",
                    requestId,
                    recruitmentInterviewDetailId,
                    reviewDecision,
                    ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save candidate decision right now.");
        }

        return "redirect:/interview-authority/internal-vacancies/request/" + requestId + "/candidates";
    }

    @PostMapping("/request/{requestId}/candidates/{recruitmentInterviewDetailId}/feedback")
    public String submitFeedback(
            @PathVariable String requestId,
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("assessmentForm") InternalInterviewAssessmentForm assessmentForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            DepartmentInterviewWorkflowDetailView workflowDetail = workflowService.getInterviewWorkflowDetail(
                    actorEmail,
                    requestId,
                    recruitmentInterviewDetailId);
            model.addAttribute("workflowDetail", workflowDetail);
            return "interview-authority/internal-vacancy-feedback-form";
        }

        try {
            workflowService.submitInterviewAssessment(
                    actorEmail,
                    requestId,
                    recruitmentInterviewDetailId,
                    toSubmissionInput(assessmentForm));
            redirectAttributes.addFlashAttribute("successMessage", "Interview feedback submitted successfully.");
        } catch (RecruitmentNotificationException ex) {
            model.addAttribute("workflowDetail", workflowService.getInterviewWorkflowDetail(
                    actorEmail,
                    requestId,
                    recruitmentInterviewDetailId));
            model.addAttribute("errorMessage", ex.getMessage());
            return "interview-authority/internal-vacancy-feedback-form";
        }

        return "redirect:/interview-authority/internal-vacancies/request/" + requestId
                + "/candidates/" + recruitmentInterviewDetailId + "/feedback";
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private InternalInterviewAssessmentForm buildAssessmentForm(
            DepartmentInterviewWorkflowDetailView workflowDetail,
            String actorEmail) {
        InternalInterviewAssessmentForm form = new InternalInterviewAssessmentForm();
        form.setInterviewAuthority(resolveInterviewAuthorityLabel(actorEmail));
        form.setInterviewDateTime(workflowDetail.getInterviewDateTime());
        form.setMobile(workflowDetail.getCandidateMobile());
        form.setEmail(workflowDetail.getCandidateEmail());
        form.setQualification(workflowDetail.getCandidateEducation());
        form.setTotalExperience(workflowDetail.getTotalExperience());
        form.setCommunicationSkillMarks(0);
        form.setTechnicalSkillMarks(0);
        form.setLeadershipQualityMarks(0);
        form.setRelevantExperienceMarks(0);
        form.setInterviewerGrade("A");
        form.setRecommendationStatus("RECOMMENDED");

        List<InternalInterviewAssessmentPanelMemberForm> panelMembers = new ArrayList<>();
        for (int index = 0; index < MAX_PANEL_MEMBER_COUNT; index++) {
            panelMembers.add(new InternalInterviewAssessmentPanelMemberForm());
        }
        form.setPanelMembers(panelMembers);

        if (workflowDetail.getAssessment() != null) {
            form.setInterviewAuthority(workflowDetail.getAssessment().getInterviewAuthority());
            form.setInterviewDateTime(workflowDetail.getAssessment().getInterviewDateTime());
            form.setMobile(workflowDetail.getAssessment().getMobile());
            form.setEmail(workflowDetail.getAssessment().getEmail());
            form.setAlternateEmail(workflowDetail.getAssessment().getAlternateEmail());
            form.setQualification(workflowDetail.getAssessment().getQualification());
            form.setTotalExperience(workflowDetail.getAssessment().getTotalExperience());
            form.setCommunicationSkillMarks(workflowDetail.getAssessment().getCommunicationSkillMarks());
            form.setTechnicalSkillMarks(workflowDetail.getAssessment().getTechnicalSkillMarks());
            form.setLeadershipQualityMarks(workflowDetail.getAssessment().getLeadershipQualityMarks());
            form.setRelevantExperienceMarks(workflowDetail.getAssessment().getRelevantExperienceMarks());
            form.setInterviewerGrade(workflowDetail.getAssessment().getInterviewerGrade());
            form.setRecommendationStatus(workflowDetail.getAssessment().getRecommendationStatus());
            form.setAssessmentRemarks(workflowDetail.getAssessment().getAssessmentRemarks());
            form.setFinalRemarks(workflowDetail.getAssessment().getFinalRemarks());

            panelMembers = new ArrayList<>();
            if (workflowDetail.getAssessment().getPanelMembers() != null) {
                for (var panelMember : workflowDetail.getAssessment().getPanelMembers()) {
                    InternalInterviewAssessmentPanelMemberForm panelMemberForm =
                            new InternalInterviewAssessmentPanelMemberForm();
                    panelMemberForm.setPanelMemberName(panelMember.getPanelMemberName());
                    panelMemberForm.setPanelMemberDesignation(panelMember.getPanelMemberDesignation());
                    panelMembers.add(panelMemberForm);
                }
            }
            while (panelMembers.size() < MAX_PANEL_MEMBER_COUNT) {
                panelMembers.add(new InternalInterviewAssessmentPanelMemberForm());
            }
            if (panelMembers.size() > MAX_PANEL_MEMBER_COUNT) {
                panelMembers = new ArrayList<>(panelMembers.subList(0, MAX_PANEL_MEMBER_COUNT));
            }
            form.setPanelMembers(panelMembers);
        }

        return form;
    }

    private DepartmentInterviewAssessmentSubmissionInput toSubmissionInput(InternalInterviewAssessmentForm form) {
        List<DepartmentInterviewAssessmentPanelMemberInput> panelMembers = new ArrayList<>();
        if (form.getPanelMembers() != null) {
            form.getPanelMembers().forEach(panelMember -> panelMembers.add(
                    DepartmentInterviewAssessmentPanelMemberInput.builder()
                            .panelMemberName(panelMember.getPanelMemberName())
                            .panelMemberDesignation(panelMember.getPanelMemberDesignation())
                            .build()));
        }

        return DepartmentInterviewAssessmentSubmissionInput.builder()
                .interviewAuthority(form.getInterviewAuthority())
                .interviewDateTime(form.getInterviewDateTime())
                .mobile(form.getMobile())
                .email(form.getEmail())
                .alternateEmail(form.getAlternateEmail())
                .qualification(form.getQualification())
                .totalExperience(form.getTotalExperience())
                .communicationSkillMarks(form.getCommunicationSkillMarks())
                .technicalSkillMarks(form.getTechnicalSkillMarks())
                .leadershipQualityMarks(form.getLeadershipQualityMarks())
                .relevantExperienceMarks(form.getRelevantExperienceMarks())
                .interviewerGrade(form.getInterviewerGrade())
                .recommendationStatus(form.getRecommendationStatus())
                .assessmentRemarks(form.getAssessmentRemarks())
                .finalRemarks(form.getFinalRemarks())
                .panelMembers(panelMembers)
                .build();
    }

    private String resolveInterviewAuthorityLabel(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            return "Internal Interview Authority";
        }

        return userRepository.findByEmailIgnoreCase(actorEmail.trim())
                .map(this::resolveUserDisplayName)
                .orElse(actorEmail.trim());
    }

    private String resolveUserDisplayName(User user) {
        if (user == null) {
            return "Internal Interview Authority";
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail().trim();
        }
        return "Internal Interview Authority";
    }
}
