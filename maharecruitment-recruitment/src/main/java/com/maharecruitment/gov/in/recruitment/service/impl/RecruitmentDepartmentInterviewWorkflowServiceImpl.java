package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentDepartmentInterviewWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentPanelMemberInput;

@Service
@Transactional(readOnly = true)
public class RecruitmentDepartmentInterviewWorkflowServiceImpl
        implements RecruitmentDepartmentInterviewWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentDepartmentInterviewWorkflowServiceImpl.class);

    private static final String FINAL_DECISION_SELECTED = "SELECTED";
    private static final String FINAL_DECISION_REJECTED = "REJECTED";
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository;
    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;

    public RecruitmentDepartmentInterviewWorkflowServiceImpl(
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository,
            AgencyCandidatePreOnboardingRepository preOnboardingRepository) {
        this.interviewDetailRepository = interviewDetailRepository;
        this.assessmentFeedbackRepository = assessmentFeedbackRepository;
        this.preOnboardingRepository = preOnboardingRepository;
    }

    @Override
    public DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            Long departmentRegistrationId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        validateIds(departmentRegistrationId, recruitmentNotificationId, recruitmentInterviewDetailId);

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findByIdForDepartmentInterviewWorkflowView(
                        departmentRegistrationId,
                        recruitmentNotificationId,
                        recruitmentInterviewDetailId)
                .orElseThrow(
                        () -> new RecruitmentNotificationException("Candidate record not found for this department."));
        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository.findByCandidateForDepartment(
                departmentRegistrationId,
                recruitmentInterviewDetailId).orElse(null);

        RecruitmentDesignationVacancyEntity vacancy = candidate.getDesignationVacancy();
        long vacancyCount = safePositive(vacancy != null ? vacancy.getNumberOfVacancy() : null);
        long filledCount = safePositive(vacancy != null ? vacancy.getFillPost() : null);
        long remainingCount = Math.max(vacancyCount - filledCount, 0L);
        var preOnboarding = preOnboardingRepository.findByInterviewDetailRecruitmentInterviewDetailId(
                recruitmentInterviewDetailId).orElse(null);
        boolean onboardingCompleted = preOnboarding != null && preOnboarding.getOnboardedAt() != null;

        String finalDecisionStatus = normalizeUpper(candidate.getFinalDecisionStatus());
        boolean selectionAllowed = true;

        return DepartmentInterviewWorkflowDetailView.builder()
                .recruitmentNotificationId(recruitmentNotificationId)
                .departmentProjectApplicationId(
                        candidate.getRecruitmentNotification().getDepartmentProjectApplicationId())
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
                .finalDecisionStatus(finalDecisionStatus)
                .finalDecisionRemarks(candidate.getFinalDecisionRemarks())
                .finalDecisionAt(candidate.getFinalDecisionAt())
                .onboardingCompleted(onboardingCompleted)
                .onboardedAt(preOnboarding != null ? preOnboarding.getOnboardedAt() : null)
                .vacancyCount(vacancyCount)
                .filledVacancyCount(filledCount)
                .remainingVacancyCount(remainingCount)
                .selectionAllowed(selectionAllowed)
                .assessment(toAssessmentView(assessment))
                .build();
    }

    @Override
    @Transactional
    public void requestInterviewTimeChange(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String changeReason) {
        validateIds(departmentRegistrationId, recruitmentNotificationId, recruitmentInterviewDetailId);
        requirePositiveId(departmentUserId, "Department user id is required.");

        String normalizedReason = normalizeText(changeReason);
        if (!StringUtils.hasText(normalizedReason)) {
            throw new RecruitmentNotificationException("Reason is required for interview time change request.");
        }

        RecruitmentInterviewDetailEntity candidate = loadCandidateForWorkflowUpdate(
                departmentRegistrationId,
                recruitmentNotificationId,
                recruitmentInterviewDetailId);

        if (candidate.getCandidateStatus() != RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
            throw new RecruitmentNotificationException(
                    "Interview change can be requested only after interview scheduling.");
        }
        if (candidate.getInterviewDateTime() == null) {
            throw new RecruitmentNotificationException("Interview date/time is not scheduled yet.");
        }
        if (StringUtils.hasText(candidate.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Interview change cannot be requested.");
        }

        candidate.setDepartmentInterviewChangeRequested(true);
        candidate.setDepartmentInterviewChangeReason(normalizedReason);
        candidate.setDepartmentInterviewChangeRequestedAt(java.time.LocalDateTime.now());
        candidate.setDepartmentInterviewChangeRequestedByUserId(departmentUserId);
        interviewDetailRepository.save(candidate);

        log.info(
                "Department requested interview change. notificationId={}, candidateId={}, departmentUserId={}",
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                departmentUserId);
    }

    @Override
    @Transactional
    public void submitInterviewAssessment(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput) {
        validateIds(departmentRegistrationId, recruitmentNotificationId, recruitmentInterviewDetailId);
        requirePositiveId(departmentUserId, "Department user id is required.");
        validateAssessmentInput(submissionInput);

        RecruitmentInterviewDetailEntity candidate = loadCandidateForWorkflowUpdate(
                departmentRegistrationId,
                recruitmentNotificationId,
                recruitmentInterviewDetailId);

        if (candidate.getCandidateStatus() != RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
            throw new RecruitmentNotificationException(
                    "Assessment can be submitted only after interview is scheduled by agency.");
        }
        if (StringUtils.hasText(candidate.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Assessment cannot be modified.");
        }
        if (candidate.getDesignationVacancy() == null
                || candidate.getDesignationVacancy().getDesignationMst() == null) {
            throw new RecruitmentNotificationException("Candidate designation vacancy mapping is missing.");
        }

        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository.findByCandidateForDepartment(
                departmentRegistrationId,
                recruitmentInterviewDetailId).orElseGet(RecruitmentAssessmentFeedbackEntity::new);

        boolean newAssessment = assessment.getRecruitmentAssessmentFeedbackId() == null;
        if (newAssessment) {
            assessment.setRecruitmentInterviewDetail(candidate);
            assessment.setDepartmentRegistrationId(departmentRegistrationId);
            assessment.setRequestId(candidate.getRecruitmentNotification().getRequestId());
            assessment.setDepartmentProjectApplicationId(
                    candidate.getRecruitmentNotification().getDepartmentProjectApplicationId());
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
        assessment.setRelevantExperienceMarks(submissionInput.getRelevantExperienceMarks());
        assessment.setInterviewerGrade(normalizeUpper(submissionInput.getInterviewerGrade()));
        assessment.setRecommendationStatus(normalizeUpper(submissionInput.getRecommendationStatus()));
        assessment.setAssessmentRemarks(normalizeText(submissionInput.getAssessmentRemarks()));
        assessment.setFinalRemarks(normalizeText(submissionInput.getFinalRemarks()));
        assessment.setInterviewerUserId(departmentUserId);
        assessment.replacePanelMembers(toPanelMemberEntities(submissionInput.getPanelMembers()));

        RecruitmentAssessmentFeedbackEntity savedAssessment = assessmentFeedbackRepository.save(assessment);

        candidate.setAssessmentSubmitted(true);
        candidate.setAssessmentSubmittedAt(java.time.LocalDateTime.now());
        candidate.setAssessmentSubmittedByUserId(departmentUserId);
        candidate.setFinalDecisionStatus(null);
        candidate.setFinalDecisionAt(null);
        candidate.setFinalDecisionByUserId(null);
        candidate.setFinalDecisionRemarks(null);
        interviewDetailRepository.save(candidate);

        log.info(
                "Department assessment submitted. notificationId={}, candidateId={}, assessmentId={}, departmentUserId={}",
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                savedAssessment.getRecruitmentAssessmentFeedbackId(),
                departmentUserId);
    }

    @Override
    @Transactional
    public void applyFinalSelectionDecision(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateFinalDecision finalDecision,
            String decisionRemarks) {
        validateIds(departmentRegistrationId, recruitmentNotificationId, recruitmentInterviewDetailId);
        requirePositiveId(departmentUserId, "Department user id is required.");

        if (finalDecision == null) {
            throw new RecruitmentNotificationException("Final decision is required.");
        }

        RecruitmentInterviewDetailEntity candidate = loadCandidateForWorkflowUpdate(
                departmentRegistrationId,
                recruitmentNotificationId,
                recruitmentInterviewDetailId);

        if (!Boolean.TRUE.equals(candidate.getAssessmentSubmitted())) {
            throw new RecruitmentNotificationException("Submit assessment report before final selection decision.");
        }
        if (isCandidateOnboarded(recruitmentInterviewDetailId)) {
            throw new RecruitmentNotificationException(
                    "Candidate is already onboarded. Final decision is locked and cannot be changed.");
        }

        RecruitmentDesignationVacancyEntity candidateVacancy = candidate.getDesignationVacancy();
        if (candidateVacancy == null) {
            throw new RecruitmentNotificationException("Designation vacancy mapping not found for this candidate.");
        }
        if (candidateVacancy.getRecruitmentDesignationVacancyId() == null) {
            throw new RecruitmentNotificationException("Candidate vacancy id is missing.");
        }

        if (finalDecision == DepartmentCandidateFinalDecision.SELECT) {
            candidate.setFinalDecisionStatus(FINAL_DECISION_SELECTED);
            candidate.setCandidateStatus(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY);
        } else {
            candidate.setFinalDecisionStatus(FINAL_DECISION_REJECTED);
            candidate.setCandidateStatus(RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT);
        }

        candidate.setFinalDecisionAt(java.time.LocalDateTime.now());
        candidate.setFinalDecisionByUserId(departmentUserId);
        candidate.setFinalDecisionRemarks(normalizeText(decisionRemarks));

        interviewDetailRepository.save(candidate);

        log.info(
                "Department final decision applied. notificationId={}, candidateId={}, decision={}, departmentUserId={}",
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                finalDecision,
                departmentUserId);
    }

    private RecruitmentInterviewDetailEntity loadCandidateForWorkflowUpdate(
            Long departmentRegistrationId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        return interviewDetailRepository.findByIdForDepartmentInterviewWorkflowUpdate(
                departmentRegistrationId,
                recruitmentNotificationId,
                recruitmentInterviewDetailId).orElseThrow(
                        () -> new RecruitmentNotificationException("Candidate record not found for this department."));
    }

    private boolean isCandidateOnboarded(Long recruitmentInterviewDetailId) {
        return preOnboardingRepository.findByInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId)
                .map(preOnboarding -> preOnboarding.getOnboardedAt() != null)
                .orElse(false);
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

    private void validateIds(Long departmentRegistrationId, Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        requirePositiveId(departmentRegistrationId, "Department registration id is required.");
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private long safePositive(Long value) {
        return value == null || value < 0 ? 0L : value;
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
}
