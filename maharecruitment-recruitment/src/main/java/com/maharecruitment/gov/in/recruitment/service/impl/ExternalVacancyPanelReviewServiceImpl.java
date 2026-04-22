package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentExternalInterviewFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentExternalInterviewFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.service.ExternalVacancyPanelReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyFeedbackSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyPanelCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.ExternalVacancyPanelWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelFeedbackView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExternalVacancyPanelReviewServiceImpl implements ExternalVacancyPanelReviewService {

    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentExternalInterviewFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ExternalVacancyPanelCandidateView> getAssignedCandidatePage(String actorEmail, String search,
            Pageable pageable) {
        User user = findUserByEmail(actorEmail);
        Page<RecruitmentInterviewDetailEntity> entities = interviewDetailRepository
                .findAssignedExternalCandidatesForPanelUser(user.getId(), search, pageable);
        return entities.map(entity -> mapToCandidateView(entity, user.getId()));
    }

    @Override
    public ExternalVacancyPanelWorkflowDetailView getWorkflowDetail(String actorEmail,
            Long recruitmentInterviewDetailId) {
        User user = findUserByEmail(actorEmail);
        RecruitmentInterviewDetailEntity entity = interviewDetailRepository.findById(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Assigned interview detail not found."));

        if (!entity.getCandidateStatus().equals(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY)) {
            throw new RecruitmentNotificationException("Candidate is not in a valid state for panel interview.");
        }

        return mapToWorkflowDetailView(entity, user.getId());
    }

    @Override
    @Transactional
    public void submitFeedback(String actorEmail, Long recruitmentInterviewDetailId,
            ExternalVacancyFeedbackSubmissionInput submissionInput) {
        User user = findUserByEmail(actorEmail);
        RecruitmentInterviewDetailEntity entity = interviewDetailRepository.findById(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Assigned interview detail not found."));

        if (!entity.getCandidateStatus().equals(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY)) {
            throw new RecruitmentNotificationException("Candidate is not in a valid state for panel interview.");
        }
        if (entity.getFinalDecisionStatus() != null) {
            throw new RecruitmentNotificationException("Final decision has already been taken for this candidate.");
        }

        Optional<RecruitmentExternalInterviewFeedbackEntity> existingFeedbackOpt = feedbackRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdAndReviewerUserId(
                        recruitmentInterviewDetailId, user.getId());

        RecruitmentExternalInterviewFeedbackEntity feedbackEntity;
        if (existingFeedbackOpt.isPresent()) {
            feedbackEntity = existingFeedbackOpt.get();
        } else {
            feedbackEntity = new RecruitmentExternalInterviewFeedbackEntity();
            feedbackEntity.setRecruitmentInterviewDetail(entity);
            feedbackEntity.setReviewerUserId(user.getId());
            feedbackEntity.setReviewerName(user.getName() != null ? user.getName() : user.getEmail());
            
            // Generate highest role label or leave empty if none
            String roleLabel = user.getRoles().stream().findFirst().map(r -> r.getName().replace("ROLE_", "")).orElse("PANEL");
            feedbackEntity.setReviewerRoleLabel(roleLabel);
        }

        feedbackEntity.setCommunicationSkillMarks(submissionInput.getCommunicationSkillMarks());
        feedbackEntity.setTechnicalSkillMarks(submissionInput.getTechnicalSkillMarks());
        feedbackEntity.setLeadershipQualityMarks(submissionInput.getLeadershipQualityMarks());
        feedbackEntity.setRelevantExperienceMarks(submissionInput.getRelevantExperienceMarks());
        feedbackEntity.setInterviewerGrade(submissionInput.getInterviewerGrade());
        feedbackEntity.setRecommendationStatus(submissionInput.getRecommendationStatus());
        feedbackEntity.setAssessmentRemarks(submissionInput.getAssessmentRemarks());
        feedbackEntity.setFinalRemarks(submissionInput.getFinalRemarks());
        
        if (feedbackEntity.getSubmittedAt() == null) {
            feedbackEntity.setSubmittedAt(LocalDateTime.now());
        }

        feedbackRepository.save(feedbackEntity);

        // Update main entity assessment flag if not already set (since this means at least one feedback is submitted)
        if (entity.getAssessmentSubmitted() == null || !entity.getAssessmentSubmitted()) {
            entity.setAssessmentSubmitted(true);
            entity.setAssessmentSubmittedAt(LocalDateTime.now());
            interviewDetailRepository.save(entity);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));
    }

    private ExternalVacancyPanelCandidateView mapToCandidateView(RecruitmentInterviewDetailEntity entity, Long userId) {
        String designationName = entity.getDesignationVacancy() != null && entity.getDesignationVacancy().getDesignationMst() != null
                ? entity.getDesignationVacancy().getDesignationMst().getDesignationName()
                : "N/A";
        String levelCode = entity.getDesignationVacancy() != null ? entity.getDesignationVacancy().getLevelCode() : "N/A";
        String agencyName = entity.getAgency() != null ? entity.getAgency().getAgencyName() : "N/A";

        Optional<RecruitmentExternalInterviewFeedbackEntity> feedbackOpt = feedbackRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdAndReviewerUserId(
                        entity.getRecruitmentInterviewDetailId(), userId);

        return ExternalVacancyPanelCandidateView.builder()
                .recruitmentNotificationId(entity.getRecruitmentNotification().getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(entity.getRecruitmentInterviewDetailId())
                .requestId(entity.getRecruitmentNotification().getRequestId())
                .projectName(entity.getRecruitmentNotification().getProjectMst() != null ? entity.getRecruitmentNotification().getProjectMst().getProjectName() : "N/A")
                .candidateName(entity.getCandidateName())
                .candidateEmail(entity.getCandidateEmail())
                .candidateMobile(entity.getCandidateMobile())
                .designationName(designationName)
                .levelCode(levelCode)
                .agencyName(agencyName)
                .interviewDateTime(entity.getInterviewDateTime())
                .interviewTimeSlot(entity.getInterviewTimeSlot())
                .interviewLink(entity.getInterviewLink())
                .finalDecisionStatus(entity.getFinalDecisionStatus())
                .feedbackSubmitted(feedbackOpt.isPresent())
                .feedbackSubmittedAt(feedbackOpt.map(RecruitmentExternalInterviewFeedbackEntity::getSubmittedAt).orElse(null))
                .build();
    }

    private ExternalVacancyPanelWorkflowDetailView mapToWorkflowDetailView(RecruitmentInterviewDetailEntity entity,
            Long userId) {
        String designationName = entity.getDesignationVacancy() != null && entity.getDesignationVacancy().getDesignationMst() != null
                ? entity.getDesignationVacancy().getDesignationMst().getDesignationName()
                : "N/A";
        String levelCode = entity.getDesignationVacancy() != null ? entity.getDesignationVacancy().getLevelCode() : "N/A";
        String agencyName = entity.getAgency() != null ? entity.getAgency().getAgencyName() : "N/A";

        Optional<RecruitmentExternalInterviewFeedbackEntity> feedbackOpt = feedbackRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdAndReviewerUserId(
                        entity.getRecruitmentInterviewDetailId(), userId);

        InternalVacancyLevelTwoPanelFeedbackView myFeedback = null;
        if (feedbackOpt.isPresent()) {
            RecruitmentExternalInterviewFeedbackEntity f = feedbackOpt.get();
            myFeedback = InternalVacancyLevelTwoPanelFeedbackView.builder()
                    .feedbackId(f.getRecruitmentExternalInterviewFeedbackId())
                    .communicationSkillMarks(f.getCommunicationSkillMarks())
                    .technicalSkillMarks(f.getTechnicalSkillMarks())
                    .leadershipQualityMarks(f.getLeadershipQualityMarks())
                    .relevantExperienceMarks(f.getRelevantExperienceMarks())
                    .interviewerGrade(f.getInterviewerGrade())
                    .recommendationStatus(f.getRecommendationStatus())
                    .assessmentRemarks(f.getAssessmentRemarks())
                    .finalRemarks(f.getFinalRemarks())
                    .submittedAt(f.getSubmittedAt())
                    .build();
        }

        return ExternalVacancyPanelWorkflowDetailView.builder()
                .recruitmentNotificationId(entity.getRecruitmentNotification().getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(entity.getRecruitmentInterviewDetailId())
                .requestId(entity.getRecruitmentNotification().getRequestId())
                .projectName(entity.getRecruitmentNotification().getProjectMst() != null ? entity.getRecruitmentNotification().getProjectMst().getProjectName() : "N/A")
                .candidateName(entity.getCandidateName())
                .candidateEmail(entity.getCandidateEmail())
                .candidateMobile(entity.getCandidateMobile())
                .candidateEducation(entity.getCandidateEducation())
                .designationName(designationName)
                .levelCode(levelCode)
                .joiningTime(entity.getJoiningTime())
                .resumeFilePath(entity.getResumeFilePath())
                .agencyName(agencyName)
                .interviewDateTime(entity.getInterviewDateTime())
                .interviewTimeSlot(entity.getInterviewTimeSlot())
                .interviewLink(entity.getInterviewLink())
                .feedbackSubmitted(myFeedback != null)
                .feedbackSubmittedAt(myFeedback != null ? myFeedback.getSubmittedAt() : null)
                .myFeedback(myFeedback)
                .build();
    }
}
