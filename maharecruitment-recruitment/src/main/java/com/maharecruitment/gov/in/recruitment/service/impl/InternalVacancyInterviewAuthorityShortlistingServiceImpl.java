package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyInterviewAuthorityShortlistingService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancySubmittedCandidateView;

@Service
@Transactional(readOnly = true)
public class InternalVacancyInterviewAuthorityShortlistingServiceImpl
        implements InternalVacancyInterviewAuthorityShortlistingService {

    private static final Logger log = LoggerFactory.getLogger(
            InternalVacancyInterviewAuthorityShortlistingServiceImpl.class);

    private final UserRepository userRepository;
    private final RecruitmentNotificationRepository notificationRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;

    public InternalVacancyInterviewAuthorityShortlistingServiceImpl(
            UserRepository userRepository,
            RecruitmentNotificationRepository notificationRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.interviewDetailRepository = interviewDetailRepository;
    }

    @Override
    public List<InternalVacancyCandidateRequestSummaryView> getAssignedRequestSummaries(String actorEmail) {
        User actor = resolveActor(actorEmail);

        List<InternalVacancySubmittedCandidateView> candidates = interviewDetailRepository
                .findActiveCandidatesForInternalVacanciesByInterviewAuthorityUserId(actor.getId())
                .stream()
                .map(this::toCandidateView)
                .toList();

        Map<String, SummaryAccumulator> summaryByRequestId = new LinkedHashMap<>();
        for (InternalVacancySubmittedCandidateView candidate : candidates) {
            if (!StringUtils.hasText(candidate.getRequestId())) {
                continue;
            }

            SummaryAccumulator accumulator = summaryByRequestId.computeIfAbsent(
                    candidate.getRequestId(),
                    ignored -> new SummaryAccumulator(
                            candidate.getRequestId(),
                            candidate.getProjectName(),
                            0L,
                            0L,
                            0L,
                            0L,
                            0L,
                            0L,
                            candidate.getSubmittedAt()));
            accumulator.totalCandidates++;
            if (candidate.getCandidateStatus() == null) {
                accumulator.pendingReviewCandidates++;
            }
            if (candidate.getCandidateStatus() == RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT) {
                accumulator.shortlistedCandidates++;
            }
            if (candidate.getCandidateStatus() == RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT) {
                accumulator.rejectedCandidates++;
            }
            if (candidate.getCandidateStatus() == RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
                accumulator.interviewScheduledCandidates++;
            }
            if (Boolean.TRUE.equals(candidate.getAssessmentSubmitted())) {
                accumulator.feedbackSubmittedCandidates++;
            }
            if (candidate.getSubmittedAt() != null
                    && (accumulator.latestSubmittedAt == null
                            || candidate.getSubmittedAt().isAfter(accumulator.latestSubmittedAt))) {
                accumulator.latestSubmittedAt = candidate.getSubmittedAt();
            }
        }

        List<InternalVacancyCandidateRequestSummaryView> summaries = summaryByRequestId.values().stream()
                .map(accumulator -> InternalVacancyCandidateRequestSummaryView.builder()
                        .requestId(accumulator.requestId)
                        .projectName(accumulator.projectName)
                        .totalCandidates(accumulator.totalCandidates)
                        .pendingReviewCandidates(accumulator.pendingReviewCandidates)
                        .shortlistedCandidates(accumulator.shortlistedCandidates)
                        .rejectedCandidates(accumulator.rejectedCandidates)
                        .interviewScheduledCandidates(accumulator.interviewScheduledCandidates)
                        .feedbackSubmittedCandidates(accumulator.feedbackSubmittedCandidates)
                        .latestSubmittedAt(accumulator.latestSubmittedAt)
                        .build())
                .toList();

        log.info("Loaded interview-authority internal vacancy request summaries. userId={}, requestCount={}",
                actor.getId(), summaries.size());
        return summaries;
    }

    @Override
    public InternalVacancyCandidateListView getAssignedCandidatesByRequestId(String actorEmail, String requestId) {
        User actor = resolveActor(actorEmail);
        String normalizedRequestId = normalizeRequestId(requestId);

        RecruitmentNotificationEntity notification = notificationRepository
                .findInternalVacancyForInterviewAuthorityReview(normalizedRequestId, actor.getId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "This internal vacancy request is not assigned to the logged-in interview authority."));

        List<InternalVacancySubmittedCandidateView> candidates = interviewDetailRepository
                .findActiveCandidatesForInternalVacancyByRequestIdAndInterviewAuthorityUserId(
                        normalizedRequestId,
                        actor.getId())
                .stream()
                .map(this::toCandidateView)
                .toList();

        log.info(
                "Loaded interview-authority internal vacancy candidates. userId={}, requestId={}, candidateCount={}",
                actor.getId(),
                notification.getRequestId(),
                candidates.size());

        return InternalVacancyCandidateListView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : "-")
                .notificationStatus(notification.getStatus())
                .candidates(candidates)
                .build();
    }

    @Override
    @Transactional
    public void reviewCandidate(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks) {
        User actor = resolveActor(actorEmail);
        String normalizedRequestId = normalizeRequestId(requestId);
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");

        if (reviewDecision == null) {
            throw new RecruitmentNotificationException("Candidate review decision is required.");
        }

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findByIdForInternalVacancyInterviewAuthorityReviewUpdate(
                        normalizedRequestId,
                        recruitmentInterviewDetailId,
                        actor.getId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Candidate not found for the assigned internal vacancy interview authority."));

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
        candidate.setDepartmentShortlistedByUserId(actor.getId());
        candidate.setDepartmentShortlistRemarks(normalizedRemarks);

        interviewDetailRepository.save(candidate);

        log.info(
                "Interview-authority candidate review applied. requestId={}, candidateId={}, decision={}, userId={}",
                normalizedRequestId,
                recruitmentInterviewDetailId,
                reviewDecision,
                actor.getId());
    }

    private User resolveActor(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        return userRepository.findByEmailIgnoreCase(actorEmail.trim())
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));
    }

    private InternalVacancySubmittedCandidateView toCandidateView(RecruitmentInterviewDetailEntity candidate) {
        String designationName = candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : "-";

        return InternalVacancySubmittedCandidateView.builder()
                .recruitmentNotificationId(candidate.getRecruitmentNotification() != null
                        ? candidate.getRecruitmentNotification().getRecruitmentNotificationId()
                        : null)
                .requestId(candidate.getRecruitmentNotification() != null
                        ? candidate.getRecruitmentNotification().getRequestId()
                        : null)
                .projectName(candidate.getRecruitmentNotification() != null
                        && candidate.getRecruitmentNotification().getProjectMst() != null
                                ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                                : "-")
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
                .interviewChangeRequested(candidate.getDepartmentInterviewChangeRequested())
                .interviewChangeRequestedAt(candidate.getDepartmentInterviewChangeRequestedAt())
                .assessmentSubmitted(candidate.getAssessmentSubmitted())
                .finalDecisionStatus(candidate.getFinalDecisionStatus())
                .finalDecisionRemarks(candidate.getFinalDecisionRemarks())
                .finalDecisionAt(candidate.getFinalDecisionAt())
                .build();
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

    private String normalizeRemarks(String remarks) {
        return StringUtils.hasText(remarks) ? remarks.trim() : null;
    }

    private static final class SummaryAccumulator {
        private final String requestId;
        private final String projectName;
        private long totalCandidates;
        private long pendingReviewCandidates;
        private long shortlistedCandidates;
        private long rejectedCandidates;
        private long interviewScheduledCandidates;
        private long feedbackSubmittedCandidates;
        private LocalDateTime latestSubmittedAt;

        private SummaryAccumulator(
                String requestId,
                String projectName,
                long totalCandidates,
                long pendingReviewCandidates,
                long shortlistedCandidates,
                long rejectedCandidates,
                long interviewScheduledCandidates,
                long feedbackSubmittedCandidates,
                LocalDateTime latestSubmittedAt) {
            this.requestId = requestId;
            this.projectName = projectName;
            this.totalCandidates = totalCandidates;
            this.pendingReviewCandidates = pendingReviewCandidates;
            this.shortlistedCandidates = shortlistedCandidates;
            this.rejectedCandidates = rejectedCandidates;
            this.interviewScheduledCandidates = interviewScheduledCandidates;
            this.feedbackSubmittedCandidates = feedbackSubmittedCandidates;
            this.latestSubmittedAt = latestSubmittedAt;
        }
    }
}
