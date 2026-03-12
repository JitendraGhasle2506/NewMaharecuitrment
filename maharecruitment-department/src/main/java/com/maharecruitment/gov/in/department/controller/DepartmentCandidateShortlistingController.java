package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.dto.DepartmentCandidateFinalDecisionForm;
import com.maharecruitment.gov.in.department.dto.DepartmentInterviewAssessmentForm;
import com.maharecruitment.gov.in.department.dto.DepartmentInterviewAssessmentPanelMemberForm;
import com.maharecruitment.gov.in.department.dto.DepartmentInterviewTimeChangeForm;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentCandidateShortlistingService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentPanelMemberInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentSelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/department/candidate-shortlisting")
public class DepartmentCandidateShortlistingController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentCandidateShortlistingController.class);
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

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

    @GetMapping("/selected-candidates")
    public String selectedCandidateList(
            @RequestParam(name = "recruitmentNotificationId", required = false) Long recruitmentNotificationId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            List<DepartmentSelectedCandidateView> selectedCandidates = shortlistingService.getSelectedCandidates(
                    actorEmail,
                    recruitmentNotificationId);
            model.addAttribute("selectedCandidates", selectedCandidates);
            model.addAttribute("selectedRecruitmentNotificationId", recruitmentNotificationId);
            model.addAttribute("selectedCandidatesTitle",
                    recruitmentNotificationId == null ? "All Selected Candidates" : "Selected Candidates (Project)");
            return "department/candidate-shortlisting-selected-candidate-list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load selected candidates. recruitmentNotificationId={}, reason={}",
                    recruitmentNotificationId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/candidate-shortlisting/projects";
        } catch (RuntimeException ex) {
            log.error("Unexpected error while loading selected candidates. recruitmentNotificationId={}",
                    recruitmentNotificationId,
                    ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load selected candidates right now.");
            return "redirect:/department/candidate-shortlisting/projects";
        }
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

    @GetMapping("/projects/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/interview-review")
    public String interviewReviewPage(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        try {
            DepartmentInterviewWorkflowDetailView workflowDetail = shortlistingService.getInterviewWorkflowDetail(
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    actorEmail);

            model.addAttribute("workflowDetail", workflowDetail);
            if (!model.containsAttribute("interviewTimeChangeForm")) {
                model.addAttribute("interviewTimeChangeForm", new DepartmentInterviewTimeChangeForm());
            }
            if (!model.containsAttribute("assessmentForm")) {
                model.addAttribute("assessmentForm", buildAssessmentForm(workflowDetail));
            }
            if (!model.containsAttribute("finalDecisionForm")) {
                DepartmentCandidateFinalDecisionForm decisionForm = new DepartmentCandidateFinalDecisionForm();
                if (workflowDetail.getFinalDecisionStatus() != null) {
                    if ("SELECTED".equalsIgnoreCase(workflowDetail.getFinalDecisionStatus())) {
                        decisionForm.setFinalDecision(DepartmentCandidateFinalDecision.SELECT);
                    } else if ("REJECTED".equalsIgnoreCase(workflowDetail.getFinalDecisionStatus())) {
                        decisionForm.setFinalDecision(DepartmentCandidateFinalDecision.REJECT);
                    }
                    decisionForm.setDecisionRemarks(workflowDetail.getFinalDecisionRemarks());
                }
                model.addAttribute("finalDecisionForm", decisionForm);
            }

            return "department/candidate-interview-review";
        } catch (RuntimeException ex) {
            log.warn(
                    "Unable to load interview review page. notificationId={}, candidateId={}, reason={}",
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId + "/candidates";
        }
    }

    @PostMapping("/projects/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/request-time-change")
    public String requestInterviewTimeChange(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("interviewTimeChangeForm") DepartmentInterviewTimeChangeForm form,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Provide a valid reason for interview time change.");
            return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                    + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
        }

        try {
            shortlistingService.requestInterviewTimeChange(
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    form.getChangeReason(),
                    actorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Interview time change request sent to agency.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
    }

    @PostMapping("/projects/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/assessment")
    public String submitAssessment(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("assessmentForm") DepartmentInterviewAssessmentForm assessmentForm,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct assessment form values.");
            return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                    + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
        }

        try {
            shortlistingService.submitInterviewAssessment(
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    toSubmissionInput(assessmentForm),
                    actorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Assessment report submitted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
    }

    @PostMapping("/projects/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/final-decision")
    public String applyFinalDecision(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            @Valid @ModelAttribute("finalDecisionForm") DepartmentCandidateFinalDecisionForm finalDecisionForm,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Select valid final decision.");
            return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                    + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
        }

        try {
            shortlistingService.applyFinalSelectionDecision(
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    finalDecisionForm.getFinalDecision(),
                    finalDecisionForm.getDecisionRemarks(),
                    actorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Final decision saved successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/department/candidate-shortlisting/projects/" + recruitmentNotificationId
                + "/candidates/" + recruitmentInterviewDetailId + "/interview-review";
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private DepartmentInterviewAssessmentForm buildAssessmentForm(DepartmentInterviewWorkflowDetailView workflowDetail) {
        DepartmentInterviewAssessmentForm form = new DepartmentInterviewAssessmentForm();
        form.setInterviewAuthority("Department Interview Panel");
        form.setInterviewDateTime(workflowDetail.getInterviewDateTime());
        form.setMobile(workflowDetail.getCandidateMobile());
        form.setEmail(workflowDetail.getCandidateEmail());
        form.setQualification(workflowDetail.getCandidateEducation());
        form.setTotalExperience(workflowDetail.getTotalExperience());
        form.setCommunicationSkillMarks(0);
        form.setTechnicalSkillMarks(0);
        form.setRelevantExperienceMarks(0);
        form.setInterviewerGrade("A");
        form.setRecommendationStatus("RECOMMENDED");

        List<DepartmentInterviewAssessmentPanelMemberForm> panelMembers = new ArrayList<>();
        for (int index = 0; index < MAX_PANEL_MEMBER_COUNT; index++) {
            panelMembers.add(new DepartmentInterviewAssessmentPanelMemberForm());
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
            form.setRelevantExperienceMarks(workflowDetail.getAssessment().getRelevantExperienceMarks());
            form.setInterviewerGrade(workflowDetail.getAssessment().getInterviewerGrade());
            form.setRecommendationStatus(workflowDetail.getAssessment().getRecommendationStatus());
            form.setAssessmentRemarks(workflowDetail.getAssessment().getAssessmentRemarks());
            form.setFinalRemarks(workflowDetail.getAssessment().getFinalRemarks());

            panelMembers = new ArrayList<>();
            if (workflowDetail.getAssessment().getPanelMembers() != null) {
                for (var panelMember : workflowDetail.getAssessment().getPanelMembers()) {
                    DepartmentInterviewAssessmentPanelMemberForm panelMemberForm =
                            new DepartmentInterviewAssessmentPanelMemberForm();
                    panelMemberForm.setPanelMemberName(panelMember.getPanelMemberName());
                    panelMemberForm.setPanelMemberDesignation(panelMember.getPanelMemberDesignation());
                    panelMembers.add(panelMemberForm);
                }
            }
            while (panelMembers.size() < MAX_PANEL_MEMBER_COUNT) {
                panelMembers.add(new DepartmentInterviewAssessmentPanelMemberForm());
            }
            if (panelMembers.size() > MAX_PANEL_MEMBER_COUNT) {
                panelMembers = new ArrayList<>(panelMembers.subList(0, MAX_PANEL_MEMBER_COUNT));
            }
            form.setPanelMembers(panelMembers);
        }

        return form;
    }

    private DepartmentInterviewAssessmentSubmissionInput toSubmissionInput(DepartmentInterviewAssessmentForm form) {
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
                .relevantExperienceMarks(form.getRelevantExperienceMarks())
                .interviewerGrade(form.getInterviewerGrade())
                .recommendationStatus(form.getRecommendationStatus())
                .assessmentRemarks(form.getAssessmentRemarks())
                .finalRemarks(form.getFinalRemarks())
                .panelMembers(panelMembers)
                .build();
    }
}
