package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.DepartmentNotificationCandidateSummaryProjection;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentDepartmentCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentSubmittedCandidateView;

@Service
@Transactional(readOnly = true)
public class RecruitmentDepartmentCandidateReviewServiceImpl implements RecruitmentDepartmentCandidateReviewService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentDepartmentCandidateReviewServiceImpl.class);

    private final RecruitmentNotificationRepository notificationRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;

    public RecruitmentDepartmentCandidateReviewServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository) {
        this.notificationRepository = notificationRepository;
        this.interviewDetailRepository = interviewDetailRepository;
    }

    @Override
    public List<DepartmentShortlistingProjectView> getDepartmentShortlistingProjects(Long departmentRegistrationId) {
        requirePositiveId(departmentRegistrationId, "Department registration id is required.");

        List<DepartmentNotificationCandidateSummaryProjection> summaries = interviewDetailRepository
                .findDepartmentCandidateSummaries(departmentRegistrationId);

        return summaries.stream()
                .map(summary -> DepartmentShortlistingProjectView.builder()
                        .recruitmentNotificationId(summary.getRecruitmentNotificationId())
                        .requestId(summary.getRequestId())
                        .departmentProjectApplicationId(summary.getDepartmentProjectApplicationId())
                        .projectName(summary.getProjectName())
                        .totalCandidates(summary.getTotalCandidates() == null ? 0L : summary.getTotalCandidates())
                        .pendingCandidates(summary.getPendingCandidates() == null ? 0L : summary.getPendingCandidates())
                        .shortlistedCandidates(
                                summary.getShortlistedCandidates() == null ? 0L : summary.getShortlistedCandidates())
                        .rejectedCandidates(summary.getRejectedCandidates() == null ? 0L : summary.getRejectedCandidates())
                        .latestSubmittedAt(summary.getLatestSubmittedAt())
                        .build())
                .toList();
    }

    @Override
    public DepartmentShortlistingDetailView getDepartmentShortlistingDetail(
            Long departmentRegistrationId,
            Long recruitmentNotificationId) {
        requirePositiveId(departmentRegistrationId, "Department registration id is required.");
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");

        RecruitmentNotificationEntity notification = findDepartmentNotification(
                departmentRegistrationId,
                recruitmentNotificationId);

        List<DepartmentSubmittedCandidateView> candidates = interviewDetailRepository
                .findCandidatesForDepartmentReview(departmentRegistrationId, recruitmentNotificationId)
                .stream()
                .map(this::toCandidateView)
                .toList();

        return DepartmentShortlistingDetailView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .requestId(notification.getRequestId())
                .departmentProjectApplicationId(notification.getDepartmentProjectApplicationId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : null)
                .candidates(candidates)
                .build();
    }

    @Override
    @Transactional
    public void applyCandidateReviewDecision(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks) {
        requirePositiveId(departmentRegistrationId, "Department registration id is required.");
        requirePositiveId(departmentUserId, "Department user id is required.");
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");

        if (reviewDecision == null) {
            throw new RecruitmentNotificationException("Candidate review decision is required.");
        }

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository.findByIdForDepartmentReviewUpdate(
                departmentRegistrationId,
                recruitmentNotificationId,
                recruitmentInterviewDetailId).orElseThrow(
                        () -> new RecruitmentNotificationException("Candidate not found for department review."));

        if (candidate.getCandidateStatus() == RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
            throw new RecruitmentNotificationException("Candidate cannot be reviewed after interview scheduling.");
        }

        String normalizedRemarks = normalizeRemarks(reviewRemarks);
        if (reviewDecision == DepartmentCandidateReviewDecision.REJECT && !StringUtils.hasText(normalizedRemarks)) {
            throw new RecruitmentNotificationException("Remarks are required when rejecting a candidate.");
        }

        RecruitmentCandidateStatus nextStatus = reviewDecision == DepartmentCandidateReviewDecision.SHORTLIST
                ? RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT
                : RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT;

        candidate.setCandidateStatus(nextStatus);
        candidate.setDepartmentShortlistedAt(LocalDateTime.now());
        candidate.setDepartmentShortlistedByUserId(departmentUserId);
        candidate.setDepartmentShortlistRemarks(normalizedRemarks);

        interviewDetailRepository.save(candidate);

        log.info(
                "Department candidate review applied. recruitmentNotificationId={}, candidateId={}, decision={}, departmentUserId={}",
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                reviewDecision,
                departmentUserId);
    }

    private RecruitmentNotificationEntity findDepartmentNotification(
            Long departmentRegistrationId,
            Long recruitmentNotificationId) {
        return notificationRepository.findForDepartmentReview(departmentRegistrationId, recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for this department."));
    }

    private DepartmentSubmittedCandidateView toCandidateView(RecruitmentInterviewDetailEntity candidate) {
        String designationName = candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : "-";

        return DepartmentSubmittedCandidateView.builder()
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .agencyId(candidate.getAgency() != null ? candidate.getAgency().getAgencyId() : null)
                .agencyName(candidate.getAgency() != null ? candidate.getAgency().getAgencyName() : "-")
                .designationVacancyId(candidate.getDesignationVacancy() != null
                        ? candidate.getDesignationVacancy().getRecruitmentDesignationVacancyId()
                        : null)
                .designationName(designationName)
                .levelCode(candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .resumeOriginalName(candidate.getResumeOriginalName())
                .resumeFilePath(candidate.getResumeFilePath())
                .candidateStatus(candidate.getCandidateStatus())
                .departmentShortlistRemarks(candidate.getDepartmentShortlistRemarks())
                .submittedAt(candidate.getCreatedDateTime())
                .interviewDateTime(candidate.getInterviewDateTime())
                .interviewTimeSlot(candidate.getInterviewTimeSlot())
                .interviewLink(candidate.getInterviewLink())
                .build();
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private String normalizeRemarks(String remarks) {
        return StringUtils.hasText(remarks) ? remarks.trim() : null;
    }
}
