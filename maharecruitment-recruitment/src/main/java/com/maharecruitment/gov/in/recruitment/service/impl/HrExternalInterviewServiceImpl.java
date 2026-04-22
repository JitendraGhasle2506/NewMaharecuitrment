package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentExternalInterviewFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentExternalInterviewPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentExternalInterviewFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentExternalInterviewPanelMemberRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.service.HrExternalInterviewService;
import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.HrExternalInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelFeedbackView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelMemberView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelUserOptionView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HrExternalInterviewServiceImpl implements HrExternalInterviewService {

    private final RecruitmentInterviewDetailRepository recruitmentInterviewDetailRepository;
    private final RecruitmentExternalInterviewPanelMemberRepository panelMemberRepository;
    private final RecruitmentExternalInterviewFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    private static final List<String> ALLOWED_PANEL_ROLE_NAMES = List.of("ROLE_STM", "ROLE_COO", "ROLE_HOD", "ROLE_HR", "ROLE_PM", "ROLE_EMPLOYEE");
    private static final Map<String, String> ALLOWED_PANEL_ROLE_LABELS = Map.of(
            "ROLE_STM", "STM",
            "ROLE_COO", "COO",
            "ROLE_HOD", "HOD",
            "ROLE_HR", "HR",
            "ROLE_PM", "PM",
            "ROLE_EMPLOYEE", "EMP");

    // Scoring constants
    private static final int CRITERIA_COUNT      = 4;   // comm + tech + leadership + relExp
    private static final int MAX_MARK_PER_CRITERIA = 5;
    private static final double PASSING_PERCENTAGE = 60.0;
    private static final String DECISION_SELECTED  = "SELECTED";
    private static final String DECISION_REJECTED  = "REJECTED";
    private static final int MIN_ASSESSMENTS       = 2;

    @Override
    public List<HrExternalInterviewCandidateView> getExternalInterviewsForHr() {
        List<RecruitmentInterviewDetailEntity> entities = recruitmentInterviewDetailRepository.findExternalInterviewsForHR();
        return entities.stream().map(this::mapToView).collect(Collectors.toList());
    }

    @Override
    public List<InternalVacancyLevelTwoPanelUserOptionView> getEligiblePanelUsers() {
        List<User> users = userRepository.findDistinctUsersByRoleNames(ALLOWED_PANEL_ROLE_NAMES);
        
        // Fetch active employees to map designations
        Map<String, String> emailToDesignation = employeeRepository.findByStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc("ACTIVE")
                .stream()
                .filter(e -> e.getEmail() != null)
                .collect(Collectors.toMap(
                        e -> e.getEmail().toLowerCase().trim(),
                        e -> e.getDesignation() != null ? e.getDesignation().getDesignationName() : "N/A",
                        (v1, v2) -> v1 // handle duplicates if any
                ));

        return users.stream()
                .map(u -> toEligiblePanelUserView(u, emailToDesignation))
                .sorted(Comparator.comparing(u -> u.getName().toLowerCase()))
                .toList();
    }

    @Override
    @Transactional
    public void assignInterviewPanel(Long recruitmentInterviewDetailId, List<Long> panelUserIds, String actorEmail) {
        RecruitmentInterviewDetailEntity entity = recruitmentInterviewDetailRepository.findById(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("External interview detail not found."));

        if (panelUserIds == null || panelUserIds.isEmpty()) {
            throw new RecruitmentNotificationException("At least two panel members are required.");
        }
        if (panelUserIds.size() < 2) {
             throw new RecruitmentNotificationException("At least two panel members are required.");
        }
        if (panelUserIds.size() > 5) {
             throw new RecruitmentNotificationException("Maximum five panel members are allowed.");
        }

        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Actor user not found."));

        List<User> users = userRepository.findAllById(panelUserIds);
        if (users.size() != panelUserIds.size()) {
            throw new RecruitmentNotificationException("One or more selected users not found.");
        }

        panelMemberRepository.deleteByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId);

        List<RecruitmentExternalInterviewPanelMemberEntity> panelMembers = users.stream().map(user -> {
            RecruitmentExternalInterviewPanelMemberEntity member = new RecruitmentExternalInterviewPanelMemberEntity();
            member.setRecruitmentInterviewDetail(entity);
            member.setPanelUserId(user.getId());
            member.setPanelMemberName(user.getName() != null ? user.getName() : user.getEmail());
            member.setPanelMemberDesignation(resolvePanelRoleSummary(user));
            return member;
        }).collect(Collectors.toList());

        panelMemberRepository.saveAll(panelMembers);
    }

    @Override
    public HrExternalInterviewWorkflowDetailView getInterviewWorkflowDetail(Long recruitmentInterviewDetailId) {
        RecruitmentInterviewDetailEntity entity = recruitmentInterviewDetailRepository.findById(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("External interview detail not found."));

        List<RecruitmentExternalInterviewPanelMemberEntity> panelMembers = panelMemberRepository.findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId);
        List<RecruitmentExternalInterviewFeedbackEntity> feedbacks = feedbackRepository.findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId);

        // Compute average score
        int awarded   = computeTotalMarksAwarded(feedbacks);
        int possible  = CRITERIA_COUNT * MAX_MARK_PER_CRITERIA * feedbacks.size();
        double pct    = possible > 0 ? (awarded * 100.0) / possible : 0.0;
        String computed = feedbacks.size() >= MIN_ASSESSMENTS
                ? (pct >= PASSING_PERCENTAGE ? DECISION_SELECTED : DECISION_REJECTED)
                : null;

        return HrExternalInterviewWorkflowDetailView.builder()
                .recruitmentNotificationId(entity.getRecruitmentNotification().getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(entity.getRecruitmentInterviewDetailId())
                .requestId(entity.getRecruitmentNotification().getRequestId())
                .projectName(entity.getRecruitmentNotification().getProjectMst() != null ? entity.getRecruitmentNotification().getProjectMst().getProjectName() : "N/A")
                .candidateName(entity.getCandidateName())
                .candidateEmail(entity.getCandidateEmail())
                .candidateMobile(entity.getCandidateMobile())
                .candidateEducation(entity.getCandidateEducation())
                .designationName(entity.getDesignationVacancy() != null && entity.getDesignationVacancy().getDesignationMst() != null ? entity.getDesignationVacancy().getDesignationMst().getDesignationName() : "N/A")
                .levelCode(entity.getDesignationVacancy() != null ? entity.getDesignationVacancy().getLevelCode() : "N/A")
                .joiningTime(entity.getJoiningTime())
                .resumeFilePath(entity.getResumeFilePath())
                .finalDecisionStatus(entity.getFinalDecisionStatus())
                .interviewDateTime(entity.getInterviewDateTime())
                .interviewTimeSlot(entity.getInterviewTimeSlot())
                .interviewLink(entity.getInterviewLink())
                .agencyName(entity.getAgency() != null ? entity.getAgency().getAgencyName() : "N/A")
                .panelAssigned(!panelMembers.isEmpty())
                .panelMembers(panelMembers.stream().map(m -> InternalVacancyLevelTwoPanelMemberView.builder()
                        .panelUserId(m.getPanelUserId())
                        .panelMemberName(m.getPanelMemberName())
                        .panelMemberDesignation(m.getPanelMemberDesignation())
                        .build()).collect(Collectors.toList()))
                .panelFeedbackSubmittedCount(feedbacks.size())
                .panelFeedbacks(feedbacks.stream().map(f -> InternalVacancyLevelTwoPanelFeedbackView.builder()
                        .feedbackId(f.getRecruitmentExternalInterviewFeedbackId())
                        .reviewerUserId(f.getReviewerUserId())
                        .reviewerName(f.getReviewerName())
                        .reviewerRoleLabel(f.getReviewerRoleLabel())
                        .communicationSkillMarks(f.getCommunicationSkillMarks())
                        .technicalSkillMarks(f.getTechnicalSkillMarks())
                        .leadershipQualityMarks(f.getLeadershipQualityMarks())
                        .relevantExperienceMarks(f.getRelevantExperienceMarks())
                        .interviewerGrade(f.getInterviewerGrade())
                        .recommendationStatus(f.getRecommendationStatus())
                        .assessmentRemarks(f.getAssessmentRemarks())
                        .finalRemarks(f.getFinalRemarks())
                        .submittedAt(f.getSubmittedAt())
                        .build()).collect(Collectors.toList()))
                .totalMarksAwarded(awarded)
                .totalMarksPossible(possible)
                .averageScorePercentage(Math.round(pct * 10.0) / 10.0)
                .computedDecision(computed)
                .build();
    }

    @Override
    @Transactional
    public void submitHrFinalDecision(
            Long recruitmentInterviewDetailId,
            String decisionRemarks,
            String actorEmail) {

        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Acting user not found."));

        RecruitmentInterviewDetailEntity entity = recruitmentInterviewDetailRepository
                .findById(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("External interview record not found."));

        if (StringUtils.hasText(entity.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision has already been recorded for this candidate.");
        }

        List<RecruitmentExternalInterviewFeedbackEntity> feedbacks = feedbackRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId);
        if (feedbacks.size() < MIN_ASSESSMENTS) {
            throw new RecruitmentNotificationException(
                    "At least " + MIN_ASSESSMENTS + " panel assessments must be submitted before finalizing.");
        }

        // Auto-compute decision from average marks
        int awarded  = computeTotalMarksAwarded(feedbacks);
        int possible = CRITERIA_COUNT * MAX_MARK_PER_CRITERIA * feedbacks.size();
        double pct   = possible > 0 ? (awarded * 100.0) / possible : 0.0;
        String computed = pct >= PASSING_PERCENTAGE ? DECISION_SELECTED : DECISION_REJECTED;

        entity.setFinalDecisionStatus(computed);
        entity.setFinalDecisionAt(LocalDateTime.now());
        entity.setFinalDecisionByUserId(actor.getId());
        entity.setFinalDecisionRemarks(
                StringUtils.hasText(decisionRemarks) ? decisionRemarks.trim() : null);

        if (DECISION_REJECTED.equals(computed)) {
            entity.setCandidateStatus(RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT);
        }
        // SELECTED keeps INTERVIEW_SCHEDULED_BY_AGENCY (same pattern as dept workflow)

        recruitmentInterviewDetailRepository.save(entity);
    }

    /** Sums all 4 mark fields across every submitted feedback. */
    private int computeTotalMarksAwarded(List<RecruitmentExternalInterviewFeedbackEntity> feedbacks) {
        return feedbacks.stream().mapToInt(f ->
                safeInt(f.getCommunicationSkillMarks())
                + safeInt(f.getTechnicalSkillMarks())
                + safeInt(f.getLeadershipQualityMarks())
                + safeInt(f.getRelevantExperienceMarks())
        ).sum();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private HrExternalInterviewCandidateView mapToView(RecruitmentInterviewDetailEntity entity) {
        String designationName = entity.getDesignationVacancy() != null && entity.getDesignationVacancy().getDesignationMst() != null
                ? entity.getDesignationVacancy().getDesignationMst().getDesignationName()
                : "N/A";
        String levelCode = entity.getDesignationVacancy() != null && entity.getDesignationVacancy().getLevelCode() != null
                ? entity.getDesignationVacancy().getLevelCode()
                : "N/A";

        return HrExternalInterviewCandidateView.builder()
                .recruitmentNotificationId(entity.getRecruitmentNotification().getRecruitmentNotificationId())
                .requestId(entity.getRecruitmentNotification().getRequestId())
                .projectName(entity.getRecruitmentNotification().getProjectMst() != null
                        ? entity.getRecruitmentNotification().getProjectMst().getProjectName() : "N/A")
                .recruitmentInterviewDetailId(entity.getRecruitmentInterviewDetailId())
                .agencyName(entity.getAgency() != null ? entity.getAgency().getAgencyName() : "N/A")
                .candidateName(entity.getCandidateName())
                .candidateEmail(entity.getCandidateEmail())
                .candidateMobile(entity.getCandidateMobile())
                .designationName(designationName)
                .levelCode(levelCode)
                .totalExperience(entity.getTotalExperience())
                .relevantExperience(entity.getRelevantExperience())
                .resumeFilePath(entity.getResumeFilePath())
                .interviewDateTime(entity.getInterviewDateTime())
                .interviewLink(entity.getInterviewLink())
                .build();
    }

    private InternalVacancyLevelTwoPanelUserOptionView toEligiblePanelUserView(User user, Map<String, String> emailToDesignation) {
        List<String> roleLabels = resolveAllowedPanelRoleLabels(user);
        String name = user.getName() != null ? user.getName() : user.getEmail();
        String displayLabel = name + " (" + String.join(", ", roleLabels) + ")";
        
        String userEmail = user.getEmail() != null ? user.getEmail().toLowerCase().trim() : "";
        String designation = emailToDesignation.getOrDefault(userEmail, "System User");

        return InternalVacancyLevelTwoPanelUserOptionView.builder()
                .userId(user.getId())
                .name(name)
                .email(user.getEmail())
                .mobileNo(user.getMobileNo())
                .displayLabel(displayLabel)
                .roleLabelCsv(String.join(",", roleLabels))
                .roleLabels(roleLabels)
                .designationName(designation)
                .build();
    }

    private List<String> resolveAllowedPanelRoleLabels(User user) {
        if (user == null || user.getRoles() == null) return List.of();
        return user.getRoles().stream()
                .map(r -> r.getName())
                .filter(ALLOWED_PANEL_ROLE_NAMES::contains)
                .map(ALLOWED_PANEL_ROLE_LABELS::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolvePanelRoleSummary(User user) {
        List<String> labels = resolveAllowedPanelRoleLabels(user);
        return labels.isEmpty() ? "N/A" : String.join(", ", labels);
    }
}
