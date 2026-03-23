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

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancySubmittedCandidateView;

@Service
@Transactional(readOnly = true)
public class InternalVacancyCandidateReviewServiceImpl implements InternalVacancyCandidateReviewService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyCandidateReviewServiceImpl.class);

    private final RecruitmentNotificationRepository notificationRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;

    public InternalVacancyCandidateReviewServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository) {
        this.notificationRepository = notificationRepository;
        this.interviewDetailRepository = interviewDetailRepository;
    }

    @Override
    public List<InternalVacancyCandidateRequestSummaryView> getCandidateRequestSummaries() {
        List<InternalVacancySubmittedCandidateView> candidates = interviewDetailRepository
                .findActiveCandidatesForInternalVacancies()
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
                            candidate.getSubmittedAt()));
            accumulator.totalCandidates++;
            if (candidate.getCandidateStatus() != null
                    && "INTERVIEW_SCHEDULED_BY_AGENCY".equals(candidate.getCandidateStatus().name())) {
                accumulator.interviewScheduledCandidates++;
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
                        .interviewScheduledCandidates(accumulator.interviewScheduledCandidates)
                        .latestSubmittedAt(accumulator.latestSubmittedAt)
                        .build())
                .toList();

        log.info("Loaded HR internal vacancy candidate request summaries. requestCount={}", summaries.size());
        return summaries;
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

        List<InternalVacancySubmittedCandidateView> candidates = interviewDetailRepository
                .findActiveCandidatesForInternalVacancyByRequestId(normalizedRequestId)
                .stream()
                .map(this::toCandidateView)
                .toList();

        log.info(
                "Loaded HR internal vacancy submitted candidates. requestId={}, recruitmentNotificationId={}, candidateCount={}",
                notification.getRequestId(),
                notification.getRecruitmentNotificationId(),
                candidates.size());

        return InternalVacancyCandidateListView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : "-")
                .notificationStatus(notification.getStatus())
                .candidates(candidates)
                .build();
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

    private static final class SummaryAccumulator {
        private final String requestId;
        private final String projectName;
        private long totalCandidates;
        private long interviewScheduledCandidates;
        private LocalDateTime latestSubmittedAt;

        private SummaryAccumulator(
                String requestId,
                String projectName,
                long totalCandidates,
                long interviewScheduledCandidates,
                LocalDateTime latestSubmittedAt) {
            this.requestId = requestId;
            this.projectName = projectName;
            this.totalCandidates = totalCandidates;
            this.interviewScheduledCandidates = interviewScheduledCandidates;
            this.latestSubmittedAt = latestSubmittedAt;
        }
    }
}
