package com.maharecruitment.gov.in.recruitment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyPanelAssessmentEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyPanelAssessmentRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyConsolidatedAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowStatus;

@Service
@Transactional(readOnly = true)
public class InternalVacancyAssessmentServiceImpl implements InternalVacancyAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyAssessmentServiceImpl.class);

    private final InternalVacancyPanelAssessmentRepository assessmentRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository;

    public InternalVacancyAssessmentServiceImpl(
            InternalVacancyPanelAssessmentRepository assessmentRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository) {
        this.assessmentRepository = assessmentRepository;
        this.interviewDetailRepository = interviewDetailRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.levelTwoScheduleRepository = levelTwoScheduleRepository;
    }

    @Override
    @Transactional
    public void submitAssessment(InternalVacancyAssessmentCommand command, String actorEmail) {
        RecruitmentInterviewDetailEntity interview = interviewDetailRepository.findById(command.getRecruitmentInterviewDetailId())
                .orElseThrow(() -> new RecruitmentNotificationException("Interview detail not found."));

        // Resolve the current panel assessor
        Long userId = null;
        Long employeeId = null;
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(actorEmail);
        if (userOpt.isPresent()) {
            userId = userOpt.get().getId();
        } else {
            Optional<EmployeeEntity> employeeOpt = employeeRepository.findByEmail(actorEmail);
            if (employeeOpt.isPresent()) {
                employeeId = employeeOpt.get().getEmployeeId();
            } else {
                throw new RecruitmentNotificationException("Assessor not found with email: " + actorEmail);
            }
        }

        // Upsert: update existing assessment if this panel already submitted, otherwise insert new
        final Long finalUserId = userId;
        final Long finalEmployeeId = employeeId;
        InternalVacancyPanelAssessmentEntity assessment = assessmentRepository
                .findByInterviewDetailAndAssessor(command.getRecruitmentInterviewDetailId(), userId, employeeId)
                .orElseGet(() -> {
                    InternalVacancyPanelAssessmentEntity newAssessment = new InternalVacancyPanelAssessmentEntity();
                    newAssessment.setInterviewDetail(interview);
                    if (finalUserId != null) {
                        newAssessment.setAssessorUser(userOpt.get());
                    } else {
                        newAssessment.setAssessorEmployee(
                                employeeRepository.findByEmail(actorEmail)
                                        .orElseThrow(() -> new RecruitmentNotificationException("Employee not found.")));
                    }
                    return newAssessment;
                });

        assessment.setTechnicalScore(command.getTechnicalScore());
        assessment.setCommunicationScore(command.getCommunicationScore());
        assessment.setLeadershipScore(command.getLeadershipScore());
        assessment.setRelevantExperienceScore(command.getRelevantExperienceScore());
        assessment.setRemarks(command.getRemarks());
        assessment.setInterviewerGrade(command.getInterviewerGrade());
        assessment.setRecommendationStatus(command.getRecommendationStatus());
        assessment.setStatus("SUBMITTED");

        assessmentRepository.save(assessment);
        
        // Check if we should update the main interview status
        long submittedCount = assessmentRepository.countSubmittedAssessments(interview.getRecruitmentInterviewDetailId());
        if (submittedCount >= 2) {
            interview.setAssessmentSubmitted(true);
            interview.setAssessmentSubmittedAt(LocalDateTime.now());
            interviewDetailRepository.save(interview);
            
            // Auto-transition to Level 2 Ready
            synchronizeLevelTwoReadyState(interview);
        }

        log.info("Assessment submitted/updated for interview ID: {}, by: {}", interview.getRecruitmentInterviewDetailId(), actorEmail);
    }

    @Override
    public InternalVacancyConsolidatedAssessmentView getConsolidatedAssessment(Long interviewDetailId) {
        RecruitmentInterviewDetailEntity interview = interviewDetailRepository.findById(interviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Interview detail not found."));

        List<InternalVacancyPanelAssessmentEntity> assessments = assessmentRepository.findByInterviewDetailRecruitmentInterviewDetailId(interviewDetailId);
        
        Double avg = assessmentRepository.calculateAverageScore(interviewDetailId);
        BigDecimal averageScore = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return InternalVacancyConsolidatedAssessmentView.builder()
                .recruitmentInterviewDetailId(interviewDetailId)
                .candidateName(interview.getCandidateName())
                .requestId(interview.getRecruitmentNotification() != null ? interview.getRecruitmentNotification().getRequestId() : null)
                .submittedAssessments(assessments.size())
                .averageScore(averageScore)
                .individualAssessments(assessments.stream().map(this::toView).toList())
                .isSelectionCriteriaMet(assessments.size() >= 2 && assessments.stream().allMatch(a -> a.getTotalScore() != null && a.getTotalScore().compareTo(BigDecimal.valueOf(12.0)) >= 0))
                .build();
    }

    @Override
    public List<InternalVacancyAssessmentView> getIndividualAssessments(Long interviewDetailId) {
        return assessmentRepository.findByInterviewDetailRecruitmentInterviewDetailId(interviewDetailId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public InternalVacancyAssessmentView getMyAssessment(Long interviewDetailId, String actorEmail) {
        if (interviewDetailId == null || !StringUtils.hasText(actorEmail)) {
            return null;
        }

        Long userId = userRepository.findByEmailIgnoreCase(actorEmail.trim())
                .map(User::getId)
                .orElse(null);
        Long employeeId = null;
        if (userId == null) {
            employeeId = employeeRepository.findByEmail(actorEmail.trim())
                    .map(EmployeeEntity::getEmployeeId)
                    .orElse(null);
        }

        if (userId == null && employeeId == null) {
            return null;
        }

        return assessmentRepository
                .findByInterviewDetailAndAssessor(interviewDetailId, userId, employeeId)
                .map(this::toView)
                .orElse(null);
    }

    private InternalVacancyAssessmentView toView(InternalVacancyPanelAssessmentEntity entity) {
        String name = "Unknown";
        String type = "UNKNOWN";
        
        if (entity.getAssessorUser() != null) {
            name = entity.getAssessorUser().getName();
            type = "USER";
        } else if (entity.getAssessorEmployee() != null) {
            name = entity.getAssessorEmployee().getFullName();
            type = "EMPLOYEE";
        }

        return InternalVacancyAssessmentView.builder()
                .assessmentId(entity.getInternalVacancyPanelAssessmentId())
                .assessorName(name)
                .assessorType(type)
                .technicalScore(entity.getTechnicalScore())
                .communicationScore(entity.getCommunicationScore())
                .leadershipScore(entity.getLeadershipScore())
                .relevantExperienceScore(entity.getRelevantExperienceScore())
                .totalScore(entity.getTotalScore())
                .remarks(entity.getRemarks())
                .interviewerGrade(entity.getInterviewerGrade())
                .recommendationStatus(entity.getRecommendationStatus())
                .submittedAt(entity.getCreatedDateTime())
                .build();
    }

    private void synchronizeLevelTwoReadyState(RecruitmentInterviewDetailEntity candidate) {
        List<InternalVacancyPanelAssessmentEntity> assessments = assessmentRepository.findByInterviewDetailRecruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId());
        long count = assessments.size();
        
        // Criteria: >= 2 panels and ALL panels must be >= 12 (60% of 20)
        boolean criteriaMet = count >= 2 && assessments.stream().allMatch(a -> a.getTotalScore() != null && a.getTotalScore().doubleValue() >= 12.0);

        RecruitmentInternalLevelTwoScheduleEntity schedule = levelTwoScheduleRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .orElseGet(() -> {
                    if (criteriaMet) {
                        RecruitmentInternalLevelTwoScheduleEntity newSchedule = new RecruitmentInternalLevelTwoScheduleEntity();
                        newSchedule.setRecruitmentInterviewDetail(candidate);
                        return newSchedule;
                    }
                    return null;
                });

        if (schedule != null) {
            if (criteriaMet && schedule.getWorkflowStatus() == null) {
                schedule.setWorkflowStatus(InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2);
                levelTwoScheduleRepository.save(schedule);
            } else if (!criteriaMet && schedule.getWorkflowStatus() == InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2) {
                // If marks were updated and now it's below 60%, remove from L2 (if not yet scheduled)
                if (schedule.getScheduledAt() == null && schedule.getInterviewDateTime() == null) {
                    levelTwoScheduleRepository.delete(schedule);
                }
            }
        }
    }
}
