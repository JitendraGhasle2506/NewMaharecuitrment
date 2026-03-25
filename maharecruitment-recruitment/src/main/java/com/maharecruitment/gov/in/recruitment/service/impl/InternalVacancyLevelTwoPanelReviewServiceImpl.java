package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyLevelTwoPanelReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoFeedbackSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelFeedbackView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelMemberView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowStatusResolver;

@Service
@Transactional(readOnly = true)
public class InternalVacancyLevelTwoPanelReviewServiceImpl implements InternalVacancyLevelTwoPanelReviewService {

    private final RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository;
    private final RecruitmentInternalLevelTwoFeedbackRepository levelTwoFeedbackRepository;
    private final RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository;
    private final UserRepository userRepository;

    public InternalVacancyLevelTwoPanelReviewServiceImpl(
            RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository,
            RecruitmentInternalLevelTwoFeedbackRepository levelTwoFeedbackRepository,
            RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository,
            UserRepository userRepository) {
        this.levelTwoScheduleRepository = levelTwoScheduleRepository;
        this.levelTwoFeedbackRepository = levelTwoFeedbackRepository;
        this.assessmentFeedbackRepository = assessmentFeedbackRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<InternalVacancyLevelTwoPanelCandidateView> getAssignedCandidatePage(
            String actorEmail,
            String search,
            Pageable pageable) {
        User actor = resolveUser(actorEmail);
        Page<RecruitmentInternalLevelTwoScheduleEntity> schedulePage = levelTwoScheduleRepository
                .findAssignedCandidatePageForPanelUser(actor.getId(), normalizeSearchPattern(search), pageable);

        List<Long> candidateIds = schedulePage.getContent().stream()
                .map(schedule -> schedule.getRecruitmentInterviewDetail().getRecruitmentInterviewDetailId())
                .filter(Objects::nonNull)
                .toList();
        Map<Long, RecruitmentInternalLevelTwoFeedbackEntity> feedbackByCandidateId = candidateIds.isEmpty()
                ? Map.of()
                : levelTwoFeedbackRepository.findByCandidateIdsAndReviewerUserId(candidateIds, actor.getId()).stream()
                        .collect(Collectors.toMap(
                                feedback -> feedback.getSchedule()
                                        .getRecruitmentInterviewDetail()
                                        .getRecruitmentInterviewDetailId(),
                                Function.identity(),
                                (left, right) -> left));

        return schedulePage.map(schedule -> toCandidateView(
                schedule,
                feedbackByCandidateId.get(schedule.getRecruitmentInterviewDetail().getRecruitmentInterviewDetailId())));
    }

    @Override
    public InternalVacancyLevelTwoPanelWorkflowDetailView getWorkflowDetail(
            String actorEmail,
            Long recruitmentInterviewDetailId) {
        User actor = resolveUser(actorEmail);
        RecruitmentInternalLevelTwoScheduleEntity schedule = loadAssignedSchedule(actor.getId(), recruitmentInterviewDetailId);
        RecruitmentAssessmentFeedbackEntity initialAssessment = loadInitialAssessment(schedule);
        RecruitmentInternalLevelTwoFeedbackEntity myFeedback = levelTwoFeedbackRepository.findByCandidateIdAndReviewerUserId(
                recruitmentInterviewDetailId,
                actor.getId()).orElse(null);
        return toWorkflowDetail(schedule, initialAssessment, myFeedback);
    }

    @Override
    @Transactional
    public void submitFeedback(
            String actorEmail,
            Long recruitmentInterviewDetailId,
            InternalVacancyLevelTwoFeedbackSubmissionInput submissionInput) {
        validateSubmissionInput(submissionInput);
        User actor = resolveUser(actorEmail);
        RecruitmentInternalLevelTwoScheduleEntity schedule = loadAssignedSchedule(actor.getId(), recruitmentInterviewDetailId);
        var candidate = schedule.getRecruitmentInterviewDetail();

        if (StringUtils.hasText(candidate.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Round L2 final decision is already taken. Feedback cannot be modified.");
        }
        if (schedule.getInterviewDateTime() == null) {
            throw new RecruitmentNotificationException("Round L2 interview schedule is not available yet.");
        }

        RecruitmentInternalLevelTwoPanelMemberEntity assignedMember = schedule.getPanelMembers() == null
                ? null
                : schedule.getPanelMembers().stream()
                        .filter(member -> Objects.equals(member.getPanelUserId(), actor.getId()))
                        .findFirst()
                        .orElse(null);
        if (assignedMember == null) {
            throw new RecruitmentNotificationException("You are not assigned to this Round L2 interview panel.");
        }

        RecruitmentInternalLevelTwoFeedbackEntity feedback = levelTwoFeedbackRepository.findByCandidateIdAndReviewerUserId(
                recruitmentInterviewDetailId,
                actor.getId()).orElseGet(RecruitmentInternalLevelTwoFeedbackEntity::new);
        feedback.setSchedule(schedule);
        feedback.setReviewerUserId(actor.getId());
        feedback.setReviewerName(resolveUserLabel(actor));
        feedback.setReviewerRoleLabel(normalizeText(assignedMember.getPanelMemberDesignation()));
        feedback.setCommunicationSkillMarks(submissionInput.getCommunicationSkillMarks());
        feedback.setTechnicalSkillMarks(submissionInput.getTechnicalSkillMarks());
        feedback.setLeadershipQualityMarks(submissionInput.getLeadershipQualityMarks());
        feedback.setRelevantExperienceMarks(submissionInput.getRelevantExperienceMarks());
        feedback.setInterviewerGrade(normalizeUpper(submissionInput.getInterviewerGrade()));
        feedback.setRecommendationStatus(normalizeUpper(submissionInput.getRecommendationStatus()));
        feedback.setAssessmentRemarks(normalizeText(submissionInput.getAssessmentRemarks()));
        feedback.setFinalRemarks(normalizeText(submissionInput.getFinalRemarks()));
        feedback.setSubmittedAt(LocalDateTime.now());
        levelTwoFeedbackRepository.save(feedback);
    }

    private RecruitmentInternalLevelTwoScheduleEntity loadAssignedSchedule(Long panelUserId, Long recruitmentInterviewDetailId) {
        requirePositiveId(recruitmentInterviewDetailId, "Candidate is required.");
        return levelTwoScheduleRepository.findDetailedInternalScheduleByCandidateIdAndPanelUserId(
                recruitmentInterviewDetailId,
                panelUserId).orElseThrow(
                        () -> new RecruitmentNotificationException("Assigned Round L2 candidate record not found."));
    }

    private RecruitmentAssessmentFeedbackEntity loadInitialAssessment(RecruitmentInternalLevelTwoScheduleEntity schedule) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var internalVacancyOpening = candidate.getRecruitmentNotification().getInternalVacancyOpening();
        if (internalVacancyOpening == null) {
            return null;
        }
        return assessmentFeedbackRepository.findByCandidateForInternalVacancy(
                internalVacancyOpening.getInternalVacancyOpeningId(),
                candidate.getRecruitmentInterviewDetailId()).orElse(null);
    }

    private InternalVacancyLevelTwoPanelCandidateView toCandidateView(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            RecruitmentInternalLevelTwoFeedbackEntity feedback) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var notification = candidate.getRecruitmentNotification();
        var vacancy = candidate.getDesignationVacancy();

        return InternalVacancyLevelTwoPanelCandidateView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .designationName(vacancy != null && vacancy.getDesignationMst() != null
                        ? vacancy.getDesignationMst().getDesignationName()
                        : null)
                .levelCode(vacancy != null ? vacancy.getLevelCode() : null)
                .levelTwoInterviewDateTime(schedule.getInterviewDateTime())
                .levelTwoInterviewTimeSlot(schedule.getInterviewTimeSlot())
                .levelTwoMeetingLink(schedule.getMeetingLink())
                .timeChangeRequested(Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()))
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .feedbackSubmitted(feedback != null)
                .feedbackSubmittedAt(feedback != null ? feedback.getSubmittedAt() : null)
                .workflowStatus(InternalVacancyLevelTwoWorkflowStatusResolver.resolveForPanel(
                        true,
                        schedule.getPanelAssignedAt() != null,
                        Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()),
                        feedback != null,
                        candidate.getFinalDecisionStatus()))
                .build();
    }

    private InternalVacancyLevelTwoPanelWorkflowDetailView toWorkflowDetail(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            RecruitmentAssessmentFeedbackEntity initialAssessment,
            RecruitmentInternalLevelTwoFeedbackEntity myFeedback) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var notification = candidate.getRecruitmentNotification();
        var vacancy = candidate.getDesignationVacancy();

        return InternalVacancyLevelTwoPanelWorkflowDetailView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .designationName(vacancy != null && vacancy.getDesignationMst() != null
                        ? vacancy.getDesignationMst().getDesignationName()
                        : null)
                .levelCode(vacancy != null ? vacancy.getLevelCode() : null)
                .joiningTime(candidate.getJoiningTime())
                .resumeFilePath(candidate.getResumeFilePath())
                .levelTwoInterviewDateTime(schedule.getInterviewDateTime())
                .levelTwoInterviewTimeSlot(schedule.getInterviewTimeSlot())
                .levelTwoMeetingLink(schedule.getMeetingLink())
                .levelTwoRemarks(schedule.getRemarks())
                .levelTwoScheduledAt(schedule.getScheduledAt())
                .timeChangeRequested(Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()))
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .initialAssessment(toAssessmentView(initialAssessment))
                .panelMembers(schedule.getPanelMembers() == null
                        ? List.of()
                        : schedule.getPanelMembers().stream()
                                .sorted(Comparator.comparing(
                                        InternalVacancyLevelTwoPanelReviewServiceImpl::resolvePanelMemberSortValue))
                                .map(this::toPanelMemberView)
                                .toList())
                .myFeedback(toPanelFeedbackView(myFeedback))
                .workflowStatus(InternalVacancyLevelTwoWorkflowStatusResolver.resolveForPanel(
                        true,
                        schedule.getPanelAssignedAt() != null,
                        Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()),
                        myFeedback != null,
                        candidate.getFinalDecisionStatus()))
                .build();
    }

    private DepartmentInterviewAssessmentView toAssessmentView(RecruitmentAssessmentFeedbackEntity assessment) {
        if (assessment == null) {
            return null;
        }
        return DepartmentInterviewAssessmentView.builder()
                .recruitmentAssessmentFeedbackId(assessment.getRecruitmentAssessmentFeedbackId())
                .interviewAuthority(assessment.getInterviewAuthority())
                .candidateName(assessment.getCandidateName())
                .interviewDateTime(assessment.getInterviewDateTime())
                .mobile(assessment.getMobile())
                .designationName(assessment.getDesignationName())
                .levelCode(assessment.getLevelCode())
                .email(assessment.getEmail())
                .alternateEmail(assessment.getAltEmail())
                .qualification(assessment.getQualification())
                .totalExperience(assessment.getTotalExperience())
                .communicationSkillMarks(assessment.getCommunicationSkillMarks())
                .technicalSkillMarks(assessment.getTechnicalSkillMarks())
                .leadershipQualityMarks(assessment.getLeadershipQualityMarks())
                .relevantExperienceMarks(assessment.getRelevantExperienceMarks())
                .interviewerGrade(assessment.getInterviewerGrade())
                .recommendationStatus(assessment.getRecommendationStatus())
                .assessmentRemarks(assessment.getAssessmentRemarks())
                .finalRemarks(assessment.getFinalRemarks())
                .submittedAt(assessment.getCreatedDateTime())
                .panelMembers(assessment.getPanelMembers() == null
                        ? List.of()
                        : assessment.getPanelMembers().stream()
                                .map(panelMember -> DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView
                                        .builder()
                                        .panelMemberName(panelMember.getPanelMemberName())
                                        .panelMemberDesignation(panelMember.getPanelMemberDesignation())
                                        .build())
                                .toList())
                .build();
    }

    private InternalVacancyLevelTwoPanelMemberView toPanelMemberView(RecruitmentInternalLevelTwoPanelMemberEntity member) {
        return InternalVacancyLevelTwoPanelMemberView.builder()
                .panelUserId(member.getPanelUserId())
                .panelMemberName(member.getPanelMemberName())
                .panelMemberDesignation(member.getPanelMemberDesignation())
                .build();
    }

    private InternalVacancyLevelTwoPanelFeedbackView toPanelFeedbackView(
            RecruitmentInternalLevelTwoFeedbackEntity feedback) {
        if (feedback == null) {
            return null;
        }
        return InternalVacancyLevelTwoPanelFeedbackView.builder()
                .feedbackId(feedback.getRecruitmentInternalLevelTwoFeedbackId())
                .reviewerUserId(feedback.getReviewerUserId())
                .reviewerName(feedback.getReviewerName())
                .reviewerRoleLabel(feedback.getReviewerRoleLabel())
                .communicationSkillMarks(feedback.getCommunicationSkillMarks())
                .technicalSkillMarks(feedback.getTechnicalSkillMarks())
                .leadershipQualityMarks(feedback.getLeadershipQualityMarks())
                .relevantExperienceMarks(feedback.getRelevantExperienceMarks())
                .interviewerGrade(feedback.getInterviewerGrade())
                .recommendationStatus(feedback.getRecommendationStatus())
                .assessmentRemarks(feedback.getAssessmentRemarks())
                .finalRemarks(feedback.getFinalRemarks())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }

    private User resolveUser(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));
    }

    private void validateSubmissionInput(InternalVacancyLevelTwoFeedbackSubmissionInput input) {
        if (input == null) {
            throw new RecruitmentNotificationException("Feedback details are required.");
        }
        validateMarks("Communication skill marks", input.getCommunicationSkillMarks());
        validateMarks("Technical skill marks", input.getTechnicalSkillMarks());
        validateMarks("Leadership quality marks", input.getLeadershipQualityMarks());
        validateMarks("Relevant experience marks", input.getRelevantExperienceMarks());
        if (!StringUtils.hasText(input.getInterviewerGrade())) {
            throw new RecruitmentNotificationException("Interviewer grade is required.");
        }
        if (!StringUtils.hasText(input.getRecommendationStatus())) {
            throw new RecruitmentNotificationException("Recommendation status is required.");
        }
    }

    private void validateMarks(String fieldLabel, Integer marks) {
        if (marks == null) {
            throw new RecruitmentNotificationException(fieldLabel + " is required.");
        }
        if (marks < 0 || marks > 5) {
            throw new RecruitmentNotificationException(fieldLabel + " must be between 0 and 5.");
        }
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private String normalizeSearchPattern(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return "%" + search.trim().toUpperCase() + "%";
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String resolveUserLabel(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        return normalizeText(user.getEmail());
    }

    private static String resolvePanelMemberSortValue(RecruitmentInternalLevelTwoPanelMemberEntity member) {
        if (member == null || !StringUtils.hasText(member.getPanelMemberName())) {
            return "";
        }
        return member.getPanelMemberName().trim().toLowerCase();
    }
}
