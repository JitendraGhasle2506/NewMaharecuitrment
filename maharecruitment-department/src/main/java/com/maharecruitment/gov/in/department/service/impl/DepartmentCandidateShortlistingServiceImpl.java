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
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;

@Service
@Transactional(readOnly = true)
public class DepartmentCandidateShortlistingServiceImpl implements DepartmentCandidateShortlistingService {

    private final RecruitmentDepartmentCandidateReviewService candidateReviewService;
    private final UserRepository userRepository;

    public DepartmentCandidateShortlistingServiceImpl(
            RecruitmentDepartmentCandidateReviewService candidateReviewService,
            UserRepository userRepository) {
        this.candidateReviewService = candidateReviewService;
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
