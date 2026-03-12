package com.maharecruitment.gov.in.recruitment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyCandidateService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySubmittedCandidateView;

@Service
@Transactional(readOnly = true)
public class RecruitmentAgencyCandidateServiceImpl implements RecruitmentAgencyCandidateService {

    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentDesignationVacancyRepository designationVacancyRepository;
    private final RecruitmentNotificationRepository notificationRepository;
    private final AgencyNotificationTrackingRepository trackingRepository;
    private final RecruitmentAgencyNotificationActionService agencyNotificationActionService;

    public RecruitmentAgencyCandidateServiceImpl(
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            RecruitmentDesignationVacancyRepository designationVacancyRepository,
            RecruitmentNotificationRepository notificationRepository,
            AgencyNotificationTrackingRepository trackingRepository,
            RecruitmentAgencyNotificationActionService agencyNotificationActionService) {
        this.interviewDetailRepository = interviewDetailRepository;
        this.designationVacancyRepository = designationVacancyRepository;
        this.notificationRepository = notificationRepository;
        this.trackingRepository = trackingRepository;
        this.agencyNotificationActionService = agencyNotificationActionService;
    }

    @Override
    public List<AgencySubmittedCandidateView> getSubmittedCandidates(Long recruitmentNotificationId, Long agencyId) {
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");
        requirePositiveId(agencyId, "Agency id is required.");

        ensureNotificationReleasedForAgency(recruitmentNotificationId, agencyId);

        return interviewDetailRepository.findActiveCandidatesByNotificationAndAgency(recruitmentNotificationId, agencyId)
                .stream()
                .map(this::toSubmittedCandidateView)
                .toList();
    }

    @Override
    public List<AgencySelectedCandidateView> getSelectedCandidates(Long agencyId) {
        requirePositiveId(agencyId, "Agency id is required.");

        return interviewDetailRepository.findSelectedCandidatesByAgency(agencyId)
                .stream()
                .map(this::toSelectedCandidateView)
                .toList();
    }

    @Override
    public List<AgencySelectedCandidateProjectView> getSelectedCandidateProjects(Long agencyId) {
        requirePositiveId(agencyId, "Agency id is required.");

        return interviewDetailRepository.findSelectedCandidateProjectSummariesByAgency(agencyId)
                .stream()
                .map(summary -> AgencySelectedCandidateProjectView.builder()
                        .recruitmentNotificationId(summary.getRecruitmentNotificationId())
                        .requestId(summary.getRequestId())
                        .projectName(summary.getProjectName())
                        .selectedCandidatesCount(summary.getSelectedCandidatesCount() == null
                                ? 0L
                                : summary.getSelectedCandidatesCount())
                        .latestDecisionAt(summary.getLatestDecisionAt())
                        .build())
                .toList();
    }

    @Override
    public List<AgencySelectedCandidateView> getSelectedCandidates(Long agencyId, Long recruitmentNotificationId) {
        requirePositiveId(agencyId, "Agency id is required.");
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");

        return interviewDetailRepository.findSelectedCandidatesByAgencyAndNotification(agencyId, recruitmentNotificationId)
                .stream()
                .map(this::toSelectedCandidateView)
                .toList();
    }

    @Override
    @Transactional
    public void submitCandidates(
            Long recruitmentNotificationId,
            Long agencyId,
            Long agencyUserId,
            Long designationVacancyId,
            List<AgencyCandidateSubmissionInput> candidateInputs) {
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");
        requirePositiveId(agencyId, "Agency id is required.");
        requirePositiveId(agencyUserId, "Agency user id is required.");
        requirePositiveId(designationVacancyId, "Designation is required.");

        if (candidateInputs == null || candidateInputs.isEmpty()) {
            throw new RecruitmentNotificationException("At least one candidate is required.");
        }

        AgencyNotificationTrackingEntity tracking = ensureNotificationReleasedForAgency(recruitmentNotificationId, agencyId);

        RecruitmentNotificationEntity notification = notificationRepository.findByIdForUpdate(recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for id: " + recruitmentNotificationId));

        if (notification.getStatus() == RecruitmentNotificationStatus.CLOSED) {
            throw new RecruitmentNotificationException("Notification is already closed.");
        }

        RecruitmentDesignationVacancyEntity designationVacancy = designationVacancyRepository
                .findByRecruitmentDesignationVacancyIdAndNotificationRecruitmentNotificationId(
                        designationVacancyId,
                        recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Selected designation is not part of this recruitment notification."));

        Set<String> emailSet = new LinkedHashSet<>();
        Set<String> mobileSet = new LinkedHashSet<>();
        List<RecruitmentInterviewDetailEntity> candidatesToPersist = new ArrayList<>();

        for (int index = 0; index < candidateInputs.size(); index++) {
            AgencyCandidateSubmissionInput normalizedInput = normalizeCandidateInput(candidateInputs.get(index));
            int rowNumber = index + 1;
            validateCandidateInput(normalizedInput, rowNumber);

            String normalizedEmail = normalizedInput.getEmail().toLowerCase(Locale.ROOT);
            if (!emailSet.add(normalizedEmail)) {
                throw new RecruitmentNotificationException("Duplicate email in row " + rowNumber + ".");
            }
            if (!mobileSet.add(normalizedInput.getMobile())) {
                throw new RecruitmentNotificationException("Duplicate mobile number in row " + rowNumber + ".");
            }

            if (interviewDetailRepository
                    .existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateEmailIgnoreCase(
                            recruitmentNotificationId,
                            agencyId,
                            normalizedEmail)) {
                throw new RecruitmentNotificationException(
                        "Candidate email already submitted for this notification: " + normalizedEmail);
            }

            if (interviewDetailRepository
                    .existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateMobile(
                            recruitmentNotificationId,
                            agencyId,
                            normalizedInput.getMobile())) {
                throw new RecruitmentNotificationException(
                        "Candidate mobile already submitted for this notification: " + normalizedInput.getMobile());
            }

            RecruitmentInterviewDetailEntity candidateEntity = new RecruitmentInterviewDetailEntity();
            candidateEntity.setRecruitmentNotification(notification);
            candidateEntity.setAgency(tracking.getAgency());
            candidateEntity.setDesignationVacancy(designationVacancy);
            candidateEntity.setAgencyUserId(agencyUserId);
            candidateEntity.setCandidateName(normalizedInput.getCandidateName());
            candidateEntity.setCandidateEmail(normalizedEmail);
            candidateEntity.setCandidateMobile(normalizedInput.getMobile());
            candidateEntity.setCandidateEducation(normalizedInput.getCandidateEducation());
            candidateEntity.setTotalExperience(normalizedInput.getTotalExperience());
            candidateEntity.setRelevantExperience(normalizedInput.getRelevantExperience());
            candidateEntity.setJoiningTime(normalizedInput.getJoiningTime());
            candidateEntity.setResumeOriginalName(normalizedInput.getResumeOriginalName());
            candidateEntity.setResumeFilePath(normalizedInput.getResumeFilePath());
            candidateEntity.setResumeFileType(normalizedInput.getResumeFileType());
            candidateEntity.setResumeFileSize(normalizedInput.getResumeFileSize());
            candidateEntity.setCandidateStatus(RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY);
            candidateEntity.setActive(true);
            candidatesToPersist.add(candidateEntity);
        }

        interviewDetailRepository.saveAll(candidatesToPersist);
        agencyNotificationActionService.submitResponse(recruitmentNotificationId, agencyId);
    }

    @Override
    @Transactional
    public void scheduleInterview(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            Long agencyId,
            Long agencyUserId,
            AgencyCandidateInterviewScheduleInput scheduleInput) {
        requirePositiveId(recruitmentNotificationId, "Recruitment notification id is required.");
        requirePositiveId(recruitmentInterviewDetailId, "Candidate id is required.");
        requirePositiveId(agencyId, "Agency id is required.");
        requirePositiveId(agencyUserId, "Agency user id is required.");

        validateInterviewScheduleInput(scheduleInput);
        ensureNotificationReleasedForAgency(recruitmentNotificationId, agencyId);

        RecruitmentInterviewDetailEntity candidateEntity = interviewDetailRepository
                .findByRecruitmentInterviewDetailIdAndRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                        recruitmentInterviewDetailId,
                        recruitmentNotificationId,
                        agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException("Candidate record not found for this notification."));

        if (candidateEntity.getCandidateStatus() != RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT
                && candidateEntity.getCandidateStatus() != RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY) {
            throw new RecruitmentNotificationException(
                    "Interview can be scheduled only for shortlisted candidates.");
        }
        if (StringUtils.hasText(candidateEntity.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException("Interview schedule cannot be modified after final department decision.");
        }

        candidateEntity.setInterviewDateTime(scheduleInput.getInterviewDateTime());
        candidateEntity.setInterviewTimeSlot(scheduleInput.getInterviewTimeSlot());
        candidateEntity.setInterviewLink(scheduleInput.getInterviewLink());
        candidateEntity.setInterviewRemarks(scheduleInput.getInterviewRemarks());
        candidateEntity.setInterviewScheduledAt(LocalDateTime.now());
        candidateEntity.setInterviewScheduledByUserId(agencyUserId);
        candidateEntity.setCandidateStatus(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY);
        candidateEntity.setDepartmentInterviewChangeRequested(false);
        candidateEntity.setDepartmentInterviewChangeReason(null);
        candidateEntity.setDepartmentInterviewChangeRequestedAt(null);
        candidateEntity.setDepartmentInterviewChangeRequestedByUserId(null);
        interviewDetailRepository.save(candidateEntity);
    }

    private AgencyNotificationTrackingEntity ensureNotificationReleasedForAgency(
            Long recruitmentNotificationId,
            Long agencyId) {
        return trackingRepository
                .findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                        recruitmentNotificationId,
                        agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Notification is not released for this agency."));
    }

    private AgencyCandidateSubmissionInput normalizeCandidateInput(AgencyCandidateSubmissionInput input) {
        if (input == null) {
            return AgencyCandidateSubmissionInput.builder().build();
        }

        String normalizedName = trim(input.getCandidateName());
        String normalizedEmail = trim(input.getEmail());
        String normalizedMobile = trim(input.getMobile());
        String normalizedEducation = trim(input.getCandidateEducation());
        String normalizedJoiningTime = trim(input.getJoiningTime());
        String normalizedResumeName = trim(input.getResumeOriginalName());
        String normalizedResumePath = trim(input.getResumeFilePath());
        String normalizedResumeType = trim(input.getResumeFileType());

        return AgencyCandidateSubmissionInput.builder()
                .candidateName(normalizedName)
                .email(normalizedEmail)
                .mobile(normalizedMobile)
                .candidateEducation(normalizedEducation)
                .totalExperience(input.getTotalExperience())
                .relevantExperience(input.getRelevantExperience())
                .joiningTime(normalizedJoiningTime)
                .resumeOriginalName(normalizedResumeName)
                .resumeFilePath(normalizedResumePath)
                .resumeFileType(normalizedResumeType)
                .resumeFileSize(input.getResumeFileSize())
                .build();
    }

    private void validateCandidateInput(AgencyCandidateSubmissionInput input, int rowNumber) {
        if (!StringUtils.hasText(input.getCandidateName())) {
            throw new RecruitmentNotificationException("Candidate name is required in row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(input.getEmail())) {
            throw new RecruitmentNotificationException("Candidate email is required in row " + rowNumber + ".");
        }
        if (!input.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RecruitmentNotificationException("Invalid candidate email in row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(input.getMobile())) {
            throw new RecruitmentNotificationException("Candidate mobile is required in row " + rowNumber + ".");
        }
        if (!input.getMobile().matches("^[0-9]{10,15}$")) {
            throw new RecruitmentNotificationException(
                    "Candidate mobile must be 10 to 15 digits in row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(input.getCandidateEducation())) {
            throw new RecruitmentNotificationException("Candidate qualification is required in row " + rowNumber + ".");
        }
        if (input.getTotalExperience() == null || input.getTotalExperience().compareTo(BigDecimal.ZERO) < 0) {
            throw new RecruitmentNotificationException("Total experience is invalid in row " + rowNumber + ".");
        }
        if (input.getRelevantExperience() == null || input.getRelevantExperience().compareTo(BigDecimal.ZERO) < 0) {
            throw new RecruitmentNotificationException("Relevant experience is invalid in row " + rowNumber + ".");
        }
        if (input.getRelevantExperience().compareTo(input.getTotalExperience()) > 0) {
            throw new RecruitmentNotificationException(
                    "Relevant experience cannot be greater than total experience in row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(input.getJoiningTime())) {
            throw new RecruitmentNotificationException("Joining time is required in row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(input.getResumeFilePath()) || !StringUtils.hasText(input.getResumeOriginalName())) {
            throw new RecruitmentNotificationException("Resume file is required in row " + rowNumber + ".");
        }
    }

    private void validateInterviewScheduleInput(AgencyCandidateInterviewScheduleInput scheduleInput) {
        if (scheduleInput == null) {
            throw new RecruitmentNotificationException("Interview schedule details are required.");
        }
        if (scheduleInput.getInterviewDateTime() == null) {
            throw new RecruitmentNotificationException("Interview date and time are required.");
        }
        if (scheduleInput.getInterviewDateTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new RecruitmentNotificationException("Interview date and time must be in the future.");
        }
        if (!StringUtils.hasText(scheduleInput.getInterviewTimeSlot())) {
            throw new RecruitmentNotificationException("Interview time slot is required.");
        }
    }

    private AgencySubmittedCandidateView toSubmittedCandidateView(RecruitmentInterviewDetailEntity candidate) {
        String designationName = candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : "-";

        return AgencySubmittedCandidateView.builder()
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .vacancyId(candidate.getDesignationVacancy().getRecruitmentDesignationVacancyId())
                .designationName(designationName)
                .levelCode(candidate.getDesignationVacancy().getLevelCode())
                .candidateStatus(candidate.getCandidateStatus())
                .resumeOriginalName(candidate.getResumeOriginalName())
                .resumeFilePath(candidate.getResumeFilePath())
                .interviewDateTime(candidate.getInterviewDateTime())
                .interviewTimeSlot(candidate.getInterviewTimeSlot())
                .interviewLink(candidate.getInterviewLink())
                .interviewChangeRequested(candidate.getDepartmentInterviewChangeRequested())
                .interviewChangeReason(candidate.getDepartmentInterviewChangeReason())
                .interviewChangeRequestedAt(candidate.getDepartmentInterviewChangeRequestedAt())
                .assessmentSubmitted(candidate.getAssessmentSubmitted())
                .finalDecisionStatus(candidate.getFinalDecisionStatus())
                .finalDecisionRemarks(candidate.getFinalDecisionRemarks())
                .finalDecisionAt(candidate.getFinalDecisionAt())
                .createdDateTime(candidate.getCreatedDateTime())
                .build();
    }

    private AgencySelectedCandidateView toSelectedCandidateView(RecruitmentInterviewDetailEntity candidate) {
        String designationName = candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : "-";

        return AgencySelectedCandidateView.builder()
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
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .designationName(designationName)
                .levelCode(candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null)
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .resumeFilePath(candidate.getResumeFilePath())
                .interviewDateTime(candidate.getInterviewDateTime())
                .interviewTimeSlot(candidate.getInterviewTimeSlot())
                .interviewLink(candidate.getInterviewLink())
                .finalDecisionAt(candidate.getFinalDecisionAt())
                .finalDecisionRemarks(candidate.getFinalDecisionRemarks())
                .build();
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
