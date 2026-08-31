package com.maharecruitment.gov.in.recruitment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyCandidateRequestSummaryMetricsProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyCandidateRequestSummaryProjection;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyPanelAssessmentRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancySubmittedCandidateView;

@Service
@Transactional(readOnly = true)
public class InternalVacancyCandidateReviewServiceImpl implements InternalVacancyCandidateReviewService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyCandidateReviewServiceImpl.class);

    private final RecruitmentNotificationRepository notificationRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final InternalVacancyPanelAssessmentRepository assessmentRepository;
    
    public InternalVacancyCandidateReviewServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            InternalVacancyPanelAssessmentRepository assessmentRepository) {
        this.notificationRepository = notificationRepository;
        this.interviewDetailRepository = interviewDetailRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public Page<InternalVacancyCandidateRequestSummaryView> getCandidateRequestSummaryPage(
            String searchText,
            Pageable pageable) {
        return interviewDetailRepository.findInternalVacancyCandidateRequestSummaryPage(
                        buildSearchPattern(searchText),
                        pageable)
                .map(this::toSummaryView);
    }

    @Override
    public InternalVacancyCandidateRequestListMetricsView getCandidateRequestSummaryMetrics(String searchText) {
        InternalVacancyCandidateRequestSummaryMetricsProjection metrics = interviewDetailRepository
                .summarizeInternalVacancyCandidateRequestMetrics(buildSearchPattern(searchText));

        return InternalVacancyCandidateRequestListMetricsView.builder()
                .requestCount(toSafeLong(metrics != null ? metrics.getRequestCount() : null))
                .totalCandidates(toSafeLong(metrics != null ? metrics.getTotalCandidates() : null))
                .pendingReviewCandidates(toSafeLong(metrics != null ? metrics.getPendingReviewCandidates() : null))
                .shortlistedCandidates(toSafeLong(metrics != null ? metrics.getShortlistedCandidates() : null))
                .rejectedCandidates(toSafeLong(metrics != null ? metrics.getRejectedCandidates() : null))
                .interviewScheduledCandidates(
                        toSafeLong(metrics != null ? metrics.getInterviewScheduledCandidates() : null))
                .feedbackSubmittedCandidates(
                        toSafeLong(metrics != null ? metrics.getFeedbackSubmittedCandidates() : null))
                .build();
    }

    @Override
    public InternalVacancyCandidateListView getSubmittedCandidatesByRequestId(String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);

        RecruitmentNotificationEntity notification = notificationRepository.findByRequestIdIgnoreCase(normalizedRequestId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for request id: " + normalizedRequestId));
        if (notification.getInternalVacancyOpening() == null) {
            throw new RecruitmentNotificationException(
                    "This request id does not belong to an internal vacancy recruitment flow.");
        }

        return buildCandidateList(notification, normalizedRequestId, true);
    }

    @Override
    public InternalVacancyCandidateListView getSubmittedCandidatesByRequestIdForOwner(
            String requestId,
            String actorEmail) {
        String normalizedRequestId = normalizeRequestId(requestId);
        String normalizedActorEmail = normalizeActorEmail(actorEmail);

        RecruitmentNotificationEntity notification = notificationRepository
                .findInternalVacancyForOwnerByRequestId(normalizedRequestId, normalizedActorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal vacancy request not found or you are not allowed to view its applications."));

        return buildCandidateList(notification, normalizedRequestId, false);
    }

    private InternalVacancyCandidateListView buildCandidateList(
            RecruitmentNotificationEntity notification,
            String normalizedRequestId,
            boolean includeAssessmentSummary) {

        List<InternalVacancySubmittedCandidateView> candidates = interviewDetailRepository
                .findActiveCandidatesForInternalVacancyByRequestId(normalizedRequestId)
                .stream()
                .map(candidate -> toCandidateView(candidate, includeAssessmentSummary))
                .toList();

        log.info(
                "Loaded internal vacancy submitted candidates. requestId={}, recruitmentNotificationId={}, candidateCount={}",
                notification.getRequestId(),
                notification.getRecruitmentNotificationId(),
                candidates.size());

        return InternalVacancyCandidateListView.builder()
                .internalVacancyOpeningId(notification.getInternalVacancyOpening().getInternalVacancyOpeningId())
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : "-")
                .notificationStatus(notification.getStatus())
                .candidates(candidates)
                .build();
    }

    private InternalVacancySubmittedCandidateView toCandidateView(
            RecruitmentInterviewDetailEntity candidate,
            boolean includeAssessmentSummary) {
        String designationName = candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : "-";

        Double avg = includeAssessmentSummary
                ? assessmentRepository.calculateAverageScore(candidate.getRecruitmentInterviewDetailId())
                : null;
        long count = includeAssessmentSummary
                ? assessmentRepository.countSubmittedAssessments(candidate.getRecruitmentInterviewDetailId())
                : 0L;

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
                .averagePanelScore(avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .submittedAssessmentCount(count)
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

    private String normalizeActorEmail(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return actorEmail.trim();
    }

    private InternalVacancyCandidateRequestSummaryView toSummaryView(
            InternalVacancyCandidateRequestSummaryProjection projection) {
        return InternalVacancyCandidateRequestSummaryView.builder()
                .requestId(projection.getRequestId())
                .projectName(projection.getProjectName())
                .totalCandidates(toSafeLong(projection.getTotalCandidates()))
                .pendingReviewCandidates(toSafeLong(projection.getPendingReviewCandidates()))
                .shortlistedCandidates(toSafeLong(projection.getShortlistedCandidates()))
                .rejectedCandidates(toSafeLong(projection.getRejectedCandidates()))
                .interviewScheduledCandidates(toSafeLong(projection.getInterviewScheduledCandidates()))
                .feedbackSubmittedCandidates(toSafeLong(projection.getFeedbackSubmittedCandidates()))
                .latestSubmittedAt(projection.getLatestSubmittedAt())
                .build();
    }

    private long toSafeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String buildSearchPattern(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "%" + value.trim().toUpperCase(Locale.ROOT) + "%";
    }
}
