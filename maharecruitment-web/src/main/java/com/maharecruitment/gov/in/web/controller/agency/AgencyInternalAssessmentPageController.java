package com.maharecruitment.gov.in.web.controller.agency;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentProjectView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyInternalAssessmentPageService;

@Controller
@RequestMapping("/agency/internal-assessments")
public class AgencyInternalAssessmentPageController {

    private final AgencyInternalAssessmentPageService pageService;

    public AgencyInternalAssessmentPageController(AgencyInternalAssessmentPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String assessmentProjects(Principal principal, Model model) {
        String actorEmail = resolveActorEmail(principal);
        List<AgencyInternalAssessmentProjectView> assessmentProjects = pageService.getAssessmentSubmittedProjects(actorEmail);
        model.addAttribute("assessmentProjects", assessmentProjects);
        model.addAttribute("requestCount", assessmentProjects.size());
        model.addAttribute("submittedTotal", assessmentProjects.stream()
                .map(AgencyInternalAssessmentProjectView::getAssessmentSubmittedCandidatesCount)
                .filter(value -> value != null)
                .mapToLong(Long::longValue)
                .sum());
        model.addAttribute("recommendedTotal", assessmentProjects.stream()
                .map(AgencyInternalAssessmentProjectView::getRecommendedCandidatesCount)
                .filter(value -> value != null)
                .mapToLong(Long::longValue)
                .sum());
        return "agency/internal-assessment-project-list";
    }

    @GetMapping("/{recruitmentNotificationId}")
    public String assessmentCandidates(
            @PathVariable Long recruitmentNotificationId,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        List<AgencyInternalAssessmentCandidateView> candidates = pageService.getAssessmentSubmittedCandidates(
                actorEmail,
                recruitmentNotificationId);
        model.addAttribute("assessmentCandidates", candidates);
        model.addAttribute("selectedRecruitmentNotificationId", recruitmentNotificationId);
        if (!candidates.isEmpty()) {
            AgencyInternalAssessmentCandidateView firstCandidate = candidates.get(0);
            model.addAttribute("selectedRequestId", firstCandidate.getRequestId());
            model.addAttribute("selectedProjectName", firstCandidate.getProjectName());
        }
        return "agency/internal-assessment-candidate-list";
    }

    @GetMapping("/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}")
    public String assessmentDetail(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            Principal principal,
            Model model) {
        String actorEmail = resolveActorEmail(principal);
        AgencyInternalAssessmentDetailView detail = pageService.getAssessmentSubmittedCandidateDetail(
                actorEmail,
                recruitmentNotificationId,
                recruitmentInterviewDetailId);
        model.addAttribute("detail", detail);
        if (!model.containsAttribute("levelTwoScheduleForm")) {
            AgencyInterviewScheduleForm scheduleForm = new AgencyInterviewScheduleForm();
            scheduleForm.setInterviewDate(detail.getLevelTwoInterviewDateTime() != null
                    ? detail.getLevelTwoInterviewDateTime().toLocalDate()
                    : null);
            scheduleForm.setInterviewTimeSlot(detail.getLevelTwoInterviewTimeSlot());
            scheduleForm.setInterviewLink(detail.getLevelTwoMeetingLink());
            scheduleForm.setInterviewRemarks(detail.getLevelTwoRemarks());
            model.addAttribute("levelTwoScheduleForm", scheduleForm);
        }
        return "agency/internal-assessment-detail";
    }

    @PostMapping("/{recruitmentNotificationId}/candidates/{recruitmentInterviewDetailId}/level-two")
    public String scheduleLevelTwoInterview(
            @PathVariable Long recruitmentNotificationId,
            @PathVariable Long recruitmentInterviewDetailId,
            AgencyInterviewScheduleForm scheduleForm,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        try {
            pageService.scheduleLevelTwoInterview(
                    actorEmail,
                    recruitmentNotificationId,
                    recruitmentInterviewDetailId,
                    scheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Level 2 interview details saved successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("levelTwoScheduleForm", scheduleForm);
        }
        return "redirect:/agency/internal-assessments/" + recruitmentNotificationId
                + "/candidates/" + recruitmentInterviewDetailId;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }
}
