package com.maharecruitment.gov.in.department.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentCandidateShortlistingService;
import com.maharecruitment.gov.in.department.service.model.DepartmentActorContext;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentDepartmentCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentDepartmentInterviewWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateFinalDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewScheduleAvailableCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentSelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;

@Service
@Transactional(readOnly = true)
public class DepartmentCandidateShortlistingServiceImpl implements DepartmentCandidateShortlistingService {

    private final RecruitmentDepartmentCandidateReviewService candidateReviewService;
    private final RecruitmentDepartmentInterviewWorkflowService interviewWorkflowService;
    private final UserRepository userRepository;

    public DepartmentCandidateShortlistingServiceImpl(
            RecruitmentDepartmentCandidateReviewService candidateReviewService,
            RecruitmentDepartmentInterviewWorkflowService interviewWorkflowService,
            UserRepository userRepository) {
        this.candidateReviewService = candidateReviewService;
        this.interviewWorkflowService = interviewWorkflowService;
        this.userRepository = userRepository;
    }

    @Override
    public List<DepartmentShortlistingProjectView> getProjectQueue(String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return candidateReviewService.getDepartmentShortlistingProjects(actorContext.getDepartmentRegistrationId());
    }

    @Override
    public DepartmentShortlistingDetailView getShortlistingDetail(Long recruitmentNotificationId, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return candidateReviewService.getDepartmentShortlistingDetail(
                actorContext.getDepartmentRegistrationId(),
                recruitmentNotificationId);
    }

    @Override
    public List<DepartmentSelectedCandidateView> getSelectedCandidates(String actorEmail, Long recruitmentNotificationId) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return candidateReviewService.getSelectedCandidates(
                actorContext.getDepartmentRegistrationId(),
                recruitmentNotificationId);
    }

    @Override
    public List<DepartmentInterviewScheduleAvailableCandidateView> getCandidatesAvailableForInterviewSchedule(
            String actorEmail,
            Long recruitmentNotificationId) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return candidateReviewService.getCandidatesAvailableForInterviewSchedule(
                actorContext.getDepartmentRegistrationId(),
                recruitmentNotificationId);
    }

    @Override
    @Transactional
    public void reviewCandidate(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        candidateReviewService.applyCandidateReviewDecision(
                actorContext.getDepartmentRegistrationId(),
                actorContext.getUserId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                reviewDecision,
                reviewRemarks);
    }

    @Override
    public DepartmentInterviewWorkflowDetailView getInterviewWorkflowDetail(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return interviewWorkflowService.getInterviewWorkflowDetail(
                actorContext.getDepartmentRegistrationId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId);
    }

    @Override
    @Transactional
    public void requestInterviewTimeChange(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            String changeReason,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        interviewWorkflowService.requestInterviewTimeChange(
                actorContext.getDepartmentRegistrationId(),
                actorContext.getUserId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                changeReason);
    }

    @Override
    @Transactional
    public void submitInterviewAssessment(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentInterviewAssessmentSubmissionInput submissionInput,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        interviewWorkflowService.submitInterviewAssessment(
                actorContext.getDepartmentRegistrationId(),
                actorContext.getUserId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                submissionInput);
    }

    @Override
    @Transactional
    public void applyFinalSelectionDecision(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateFinalDecision finalDecision,
            String decisionRemarks,
            String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        interviewWorkflowService.applyFinalSelectionDecision(
                actorContext.getDepartmentRegistrationId(),
                actorContext.getUserId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                finalDecision,
                decisionRemarks);
    }

    private DepartmentActorContext resolveDepartmentActorContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }

        User user = userRepository.findByEmail(actorEmail);
        if (user == null) {
            throw new DepartmentApplicationException("Authenticated user not found.");
        }

        DepartmentRegistrationEntity departmentRegistration = user.getDepartmentRegistrationId();
        if (departmentRegistration == null) {
            throw new DepartmentApplicationException("Department profile is not linked to this user.");
        }

        return DepartmentActorContext.builder()
                .userId(user.getId())
                .actorName(user.getName())
                .actorEmail(user.getEmail())
                .departmentId(departmentRegistration.getDepartmentId())
                .departmentRegistrationId(departmentRegistration.getDepartmentRegistrationId())
                .subDepartmentId(departmentRegistration.getSubDeptId())
                .build();
    }
}
