package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyInterviewAuthorityWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentPanelMemberInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowStatus;

@Service
@Transactional(readOnly = true)
public class InternalVacancyInterviewAuthorityWorkflowServiceImpl
        implements InternalVacancyInterviewAuthorityWorkflowService {

    private static final String RECOMMENDED_STATUS = "RECOMMENDED";
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

    private final UserRepository userRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository;
    private final RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository;

    public InternalVacancyInterviewAuthorityWorkflowServiceImpl(
            UserRepository userRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository,
            RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository) {
        this.userRepository = userRepository;
        this.interviewDetailRepository = interviewDetailRepository;
        this.assessmentFeedbackRepository = assessmentFeedbackRepository;
        this.levelTwoScheduleRepository = levelTwoScheduleRepository;
    }

    @Override
    public DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId) {
        User actor = resolveActor(actorEmail);
        String normalizedRequestId = normalizeRequestId(requestId);
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findByIdForInternalVacancyInterviewWorkflowView(
                        normalizedRequestId,
                        recruitmentInterviewDetailId,
                        actor.getId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Candidate record not found for the assigned internal interview authority."));

        Long internalVacancyOpeningId = candidate.getRecruitmentNotification() != null
                && candidate.getRecruitmentNotification().getInternalVacancyOpening() != null
                        ? candidate.getRecruitmentNotification().getInternalVacancyOpening().getInternalVacancyOpeningId()
                        : null;
        if (internalVacancyOpeningId == null) {
            throw new RecruitmentNotificationException("This candidate does not belong to an internal vacancy workflow.");
        }
        if (candidate.getCandidateStatus() != RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY
                && !Boolean.TRUE.equals(candidate.getAssessmentSubmitted())) {
            throw new RecruitmentNotificationException(
                    "Interview feedback is available only after interview scheduling.");
        }

        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository
                .findByCandidateForInternalVacancy(internalVacancyOpeningId, recruitmentInterviewDetailId)
                .orElse(null);

        RecruitmentDesignationVacancyEntity vacancy = candidate.getDesignationVacancy();
        long vacancyCount = safePositive(vacancy != null ? vacancy.getNumberOfVacancy() : null);
        long filledCount = safePositive(vacancy != null ? vacancy.getFillPost() : null);
        long remainingCount = Math.max(vacancyCount - filledCount, 0L);

        return DepartmentInterviewWorkflowDetailView.builder()
                .recruitmentNotificationId(candidate.getRecruitmentNotification().getRecruitmentNotificationId())
                .departmentProjectApplicationId(null)
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .designationVacancyId(vacancy != null ? vacancy.getRecruitmentDesignationVacancyId() : null)
                .requestId(candidate.getRecruitmentNotification().getRequestId())
                .projectName(candidate.getRecruitmentNotification().getProjectMst() != null
                        ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                        : null)
                .agencyName(candidate.getAgency() != null ? candidate.getAgency().getAgencyName() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .designationName(vacancy != null && vacancy.getDesignationMst() != null
                        ? vacancy.getDesignationMst().getDesignationName()
                        : null)
                .levelCode(vacancy != null ? vacancy.getLevelCode() : null)
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .candidateStatus(candidate.getCandidateStatus())
                .interviewDateTime(candidate.getInterviewDateTime())
                .interviewTimeSlot(candidate.getInterviewTimeSlot())
                .interviewLink(candidate.getInterviewLink())
                .interviewRemarks(candidate.getInterviewRemarks())
                .interviewChangeRequested(candidate.getDepartmentInterviewChangeRequested())
                .interviewChangeReason(candidate.getDepartmentInterviewChangeReason())
                .interviewChangeRequestedAt(candidate.getDepartmentInterviewChangeRequestedAt())
                .assessmentSubmitted(candidate.getAssessmentSubmitted())
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .finalDecisionRemarks(candidate.getFinalDecisionRemarks())
                .finalDecisionAt(candidate.getFinalDecisionAt())
                .onboardingCompleted(false)
                .onboardedAt(null)
                .vacancyCount(vacancyCount)
                .filledVacancyCount(filledCount)
                .remainingVacancyCount(remainingCount)
                .selectionAllowed(false)
                .assessment(toAssessmentView(assessment))
                .build();
    }

    @Override
    @Transactional
    public void submitInterviewAssessment(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput) {
        User actor = resolveActor(actorEmail);
        String normalizedRequestId = normalizeRequestId(requestId);
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");
        validateAssessmentInput(submissionInput);

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findByIdForInternalVacancyInterviewWorkflowUpdate(
                        normalizedRequestId,
                        recruitmentInterviewDetailId,
                        actor.getId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Candidate record not found for the assigned internal interview authority."));

        if (candidate.getCandidateStatus() != RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
            throw new RecruitmentNotificationException(
                    "Feedback can be submitted only after interview is scheduled by agency.");
        }
        if (StringUtils.hasText(candidate.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Feedback cannot be modified.");
        }
        if (candidate.getDesignationVacancy() == null
                || candidate.getDesignationVacancy().getDesignationMst() == null) {
            throw new RecruitmentNotificationException("Candidate designation vacancy mapping is missing.");
        }
        if (candidate.getRecruitmentNotification() == null
                || candidate.getRecruitmentNotification().getInternalVacancyOpening() == null) {
            throw new RecruitmentNotificationException("This candidate does not belong to an internal vacancy workflow.");
        }

        Long internalVacancyOpeningId = candidate.getRecruitmentNotification()
                .getInternalVacancyOpening()
                .getInternalVacancyOpeningId();

        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository
                .findByCandidateForInternalVacancy(internalVacancyOpeningId, recruitmentInterviewDetailId)
                .orElseGet(RecruitmentAssessmentFeedbackEntity::new);

        boolean newAssessment = assessment.getRecruitmentAssessmentFeedbackId() == null;
        if (newAssessment) {
            assessment.setRecruitmentInterviewDetail(candidate);
            assessment.setDepartmentRegistrationId(null);
            assessment.setDepartmentProjectApplicationId(null);
            assessment.setInternalVacancyOpeningId(internalVacancyOpeningId);
            assessment.setRequestId(candidate.getRecruitmentNotification().getRequestId());
            assessment.setDesignationId(candidate.getDesignationVacancy().getDesignationMst().getDesignationId());
            assessment.setDesignationName(candidate.getDesignationVacancy().getDesignationMst().getDesignationName());
            assessment.setLevelCode(candidate.getDesignationVacancy().getLevelCode());
            assessment.setCandidateName(candidate.getCandidateName());
            assessment.setMobile(candidate.getCandidateMobile());
            assessment.setEmail(candidate.getCandidateEmail());
        }

        assessment.setInterviewAuthority(normalizeText(submissionInput.getInterviewAuthority()));
        assessment.setInterviewDateTime(submissionInput.getInterviewDateTime());
        assessment.setMobile(normalizeText(submissionInput.getMobile()));
        assessment.setEmail(normalizeEmail(submissionInput.getEmail()));
        assessment.setAltEmail(normalizeEmail(submissionInput.getAlternateEmail()));
        assessment.setQualification(normalizeText(submissionInput.getQualification()));
        assessment.setTotalExperience(submissionInput.getTotalExperience());
        assessment.setCommunicationSkillMarks(submissionInput.getCommunicationSkillMarks());
        assessment.setTechnicalSkillMarks(submissionInput.getTechnicalSkillMarks());
        assessment.setLeadershipQualityMarks(submissionInput.getLeadershipQualityMarks());
        assessment.setRelevantExperienceMarks(submissionInput.getRelevantExperienceMarks());
        assessment.setInterviewerGrade(normalizeUpper(submissionInput.getInterviewerGrade()));
        assessment.setRecommendationStatus(normalizeUpper(submissionInput.getRecommendationStatus()));
        assessment.setAssessmentRemarks(normalizeText(submissionInput.getAssessmentRemarks()));
        assessment.setFinalRemarks(normalizeText(submissionInput.getFinalRemarks()));
        assessment.setInterviewerUserId(actor.getId());
        assessment.replacePanelMembers(toPanelMemberEntities(submissionInput.getPanelMembers()));

        assessmentFeedbackRepository.save(assessment);

        candidate.setAssessmentSubmitted(true);
        candidate.setAssessmentSubmittedAt(java.time.LocalDateTime.now());
        candidate.setAssessmentSubmittedByUserId(actor.getId());
        interviewDetailRepository.save(candidate);

        synchronizeLevelTwoReadyState(
                candidate,
                RECOMMENDED_STATUS.equals(normalizeUpper(assessment.getRecommendationStatus())));
    }

    private User resolveActor(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        return userRepository.findByEmailIgnoreCase(actorEmail.trim())
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));
    }

    private DepartmentInterviewAssessmentView toAssessmentView(RecruitmentAssessmentFeedbackEntity assessment) {
        if (assessment == null) {
            return null;
        }

        List<DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView> panelMemberViews = assessment
                .getPanelMembers() == null
                        ? List.of()
                        : assessment.getPanelMembers().stream()
                                .map(panelMember -> DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView
                                        .builder()
                                        .panelMemberName(panelMember.getPanelMemberName())
                                        .panelMemberDesignation(panelMember.getPanelMemberDesignation())
                                        .build())
                                .toList();

        return DepartmentInterviewAssessmentView.builder()
                .recruitmentAssessmentFeedbackId(assessment.getRecruitmentAssessmentFeedbackId())
                .interviewAuthority(resolveInterviewAuthorityLabel(assessment))
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
                .panelMembers(panelMemberViews)
                .build();
    }

    private List<RecruitmentAssessmentPanelMemberEntity> toPanelMemberEntities(
            List<DepartmentInterviewAssessmentPanelMemberInput> panelMemberInputs) {
        if (panelMemberInputs == null || panelMemberInputs.isEmpty()) {
            throw new RecruitmentNotificationException("At least two panel members are required.");
        }

        List<RecruitmentAssessmentPanelMemberEntity> panelMembers = new ArrayList<>();
        for (DepartmentInterviewAssessmentPanelMemberInput panelMemberInput : panelMemberInputs) {
            String memberName = normalizeText(panelMemberInput != null ? panelMemberInput.getPanelMemberName() : null);
            String memberDesignation = normalizeText(
                    panelMemberInput != null ? panelMemberInput.getPanelMemberDesignation() : null);

            if (!StringUtils.hasText(memberName) && !StringUtils.hasText(memberDesignation)) {
                continue;
            }

            if (!StringUtils.hasText(memberName) || !StringUtils.hasText(memberDesignation)) {
                throw new RecruitmentNotificationException("Panel member name and designation are mandatory.");
            }

            RecruitmentAssessmentPanelMemberEntity panelMember = new RecruitmentAssessmentPanelMemberEntity();
            panelMember.setPanelMemberName(memberName);
            panelMember.setPanelMemberDesignation(memberDesignation);
            panelMembers.add(panelMember);
        }

        if (panelMembers.size() < 2) {
            throw new RecruitmentNotificationException("Minimum two panel members are required.");
        }
        if (panelMembers.size() > MAX_PANEL_MEMBER_COUNT) {
            throw new RecruitmentNotificationException("Maximum five panel members are allowed.");
        }

        return panelMembers;
    }

    private void validateAssessmentInput(DepartmentInterviewAssessmentSubmissionInput input) {
        if (input == null) {
            throw new RecruitmentNotificationException("Assessment details are required.");
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

    private String normalizeRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw new RecruitmentNotificationException("Request id is required.");
        }
        return requestId.trim().toUpperCase();
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private long safePositive(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private String resolveInterviewAuthorityLabel(RecruitmentAssessmentFeedbackEntity assessment) {
        if (assessment == null) {
            return null;
        }
        if (assessment.getInterviewerUserId() != null) {
            User interviewer = userRepository.findById(assessment.getInterviewerUserId()).orElse(null);
            if (interviewer != null) {
                if (StringUtils.hasText(interviewer.getName())) {
                    return interviewer.getName().trim();
                }
                if (StringUtils.hasText(interviewer.getEmail())) {
                    return interviewer.getEmail().trim();
                }
            }
        }
        return normalizeText(assessment.getInterviewAuthority());
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    private void synchronizeLevelTwoReadyState(
            RecruitmentInterviewDetailEntity candidate,
            boolean recommendedForLevelTwo) {
        if (candidate == null || candidate.getRecruitmentInterviewDetailId() == null) {
            return;
        }

        RecruitmentInternalLevelTwoScheduleEntity schedule = levelTwoScheduleRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(
                        candidate.getRecruitmentInterviewDetailId())
                .orElse(null);

        if (recommendedForLevelTwo) {
            if (schedule == null) {
                schedule = new RecruitmentInternalLevelTwoScheduleEntity();
                schedule.setRecruitmentInterviewDetail(candidate);
            }
            if (schedule.getWorkflowStatus() == null
                    || schedule.getWorkflowStatus() == InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2) {
                schedule.setWorkflowStatus(InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2);
                levelTwoScheduleRepository.save(schedule);
            }
            return;
        }

        if (schedule != null
                && schedule.getScheduledAt() == null
                && schedule.getInterviewDateTime() == null
                && schedule.getPanelAssignedAt() == null
                && !Boolean.TRUE.equals(schedule.getHrTimeChangeRequested())
                && schedule.getWorkflowStatus() == InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2) {
            levelTwoScheduleRepository.delete(schedule);
        }
    }
}
