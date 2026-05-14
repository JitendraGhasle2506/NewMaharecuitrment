package com.maharecruitment.gov.in.recruitment.controller;

import java.math.BigDecimal;
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
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateFilterType;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/interview-authority/internal-vacancies")
@PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_PM', 'ROLE_STM', 'ROLE_EMPLOYEE')")
public class InternalVacancyInterviewAuthorityController {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyInterviewAuthorityController.class);
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

    private final UserRepository userRepository;
    private final InternalVacancyInterviewAuthorityShortlistingService shortlistingService;
    private final InternalVacancyInterviewAuthorityWorkflowService workflowService;
    private final InternalVacancyAssessmentService assessmentService;

    public InternalVacancyInterviewAuthorityController(
            UserRepository userRepository,
            InternalVacancyInterviewAuthorityShortlistingService shortlistingService,
            InternalVacancyInterviewAuthorityWorkflowService workflowService,
            InternalVacancyAssessmentService assessmentService) {
        this.userRepository = userRepository;
        this.shortlistingService = shortlistingService;
        this.workflowService = workflowService;
        this.assessmentService = assessmentService;
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
            @RequestParam(name = "filter", required = false) String filter,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            InternalVacancyCandidateFilterType filterType = InternalVacancyCandidateFilterType.fromRequestValue(filter);
            InternalVacancyCandidateListView candidateListView = shortlistingService
                    .getAssignedCandidatesByRequestId(resolveActorEmail(principal), requestId, filterType);
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
            // 1. Submit legacy assessment feedback (for backward compatibility/main record)
            workflowService.submitInterviewAssessment(
                    actorEmail,
                    requestId,
                    recruitmentInterviewDetailId,
                    toSubmissionInput(assessmentForm));
            
            // 2. Submit individual panel assessment
            InternalVacancyAssessmentCommand individualCommand = new InternalVacancyAssessmentCommand();
            individualCommand.setRecruitmentInterviewDetailId(recruitmentInterviewDetailId);
            individualCommand.setTechnicalScore(BigDecimal.valueOf(assessmentForm.getTechnicalSkillMarks()));
            individualCommand.setCommunicationScore(BigDecimal.valueOf(assessmentForm.getCommunicationSkillMarks()));
            individualCommand.setLeadershipScore(BigDecimal.valueOf(assessmentForm.getLeadershipQualityMarks()));
            individualCommand.setRelevantExperienceScore(BigDecimal.valueOf(assessmentForm.getRelevantExperienceMarks()));
            individualCommand.setRemarks(assessmentForm.getAssessmentRemarks());
            individualCommand.setInterviewerGrade(assessmentForm.getInterviewerGrade());
            individualCommand.setRecommendationStatus(assessmentForm.getRecommendationStatus());
            
            assessmentService.submitAssessment(individualCommand, actorEmail);

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
            DepartmentInterviewAssessmentView assessment = workflowDetail.getAssessment();
            // Only update the interviewer name if it's available from the panel-specific record
            if (assessment.getInterviewAuthority() != null) {
                form.setInterviewAuthority(assessment.getInterviewAuthority());
            }
            // Keep candidate details (interviewDateTime, mobile, email, qualification, totalExperience)
            // from workflowDetail (already set above) — the panel-specific view does not carry these fields.
            if (assessment.getCommunicationSkillMarks() != null) {
                form.setCommunicationSkillMarks(assessment.getCommunicationSkillMarks());
            }
            if (assessment.getTechnicalSkillMarks() != null) {
                form.setTechnicalSkillMarks(assessment.getTechnicalSkillMarks());
            }
            if (assessment.getLeadershipQualityMarks() != null) {
                form.setLeadershipQualityMarks(assessment.getLeadershipQualityMarks());
            }
            if (assessment.getRelevantExperienceMarks() != null) {
                form.setRelevantExperienceMarks(assessment.getRelevantExperienceMarks());
            }
            if (assessment.getInterviewerGrade() != null) {
                form.setInterviewerGrade(assessment.getInterviewerGrade());
            }
            if (assessment.getRecommendationStatus() != null) {
                form.setRecommendationStatus(assessment.getRecommendationStatus());
            }
            if (assessment.getAssessmentRemarks() != null) {
                form.setAssessmentRemarks(assessment.getAssessmentRemarks());
            }
            if (assessment.getFinalRemarks() != null) {
                form.setFinalRemarks(assessment.getFinalRemarks());
            }

            // Restore panel members if provided
            if (assessment.getPanelMembers() != null && !assessment.getPanelMembers().isEmpty()) {
                panelMembers = new ArrayList<>();
                for (var panelMember : assessment.getPanelMembers()) {
                    InternalInterviewAssessmentPanelMemberForm panelMemberForm =
                            new InternalInterviewAssessmentPanelMemberForm();
                    panelMemberForm.setPanelMemberName(panelMember.getPanelMemberName());
                    panelMemberForm.setPanelMemberDesignation(panelMember.getPanelMemberDesignation());
                    panelMembers.add(panelMemberForm);
                }
                while (panelMembers.size() < MAX_PANEL_MEMBER_COUNT) {
                    panelMembers.add(new InternalInterviewAssessmentPanelMemberForm());
                }
                if (panelMembers.size() > MAX_PANEL_MEMBER_COUNT) {
                    panelMembers = new ArrayList<>(panelMembers.subList(0, MAX_PANEL_MEMBER_COUNT));
                }
                form.setPanelMembers(panelMembers);
            }
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
