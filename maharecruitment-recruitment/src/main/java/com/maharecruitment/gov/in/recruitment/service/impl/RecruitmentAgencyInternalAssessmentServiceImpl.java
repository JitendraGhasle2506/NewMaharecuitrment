package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyInternalAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;

@Service
@Transactional(readOnly = true)
public class RecruitmentAgencyInternalAssessmentServiceImpl implements RecruitmentAgencyInternalAssessmentService {

    private static final String RECOMMENDED_STATUS = "RECOMMENDED";

    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository;
    private final RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository;
    private final UserRepository userRepository;

    public RecruitmentAgencyInternalAssessmentServiceImpl(
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository,
            RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository,
            UserRepository userRepository) {
        this.interviewDetailRepository = interviewDetailRepository;
        this.assessmentFeedbackRepository = assessmentFeedbackRepository;
        this.levelTwoScheduleRepository = levelTwoScheduleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<AgencyInternalAssessmentProjectView> getAssessmentSubmittedProjects(Long agencyId) {
        requirePositiveId(agencyId, "Agency is required.");
        List<RecruitmentInterviewDetailEntity> candidates = interviewDetailRepository
                .findInternalAssessmentSubmittedCandidatesByAgency(agencyId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, RecruitmentAssessmentFeedbackEntity> assessmentMap = loadAssessmentMap(candidates);

        return candidates.stream()
                .collect(Collectors.groupingBy(
                        candidate -> candidate.getRecruitmentNotification().getRecruitmentNotificationId(),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(groupedCandidates -> toProjectView(groupedCandidates, assessmentMap))
                .sorted(Comparator.comparing(
                        AgencyInternalAssessmentProjectView::getLatestAssessmentSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<AgencyInternalAssessmentCandidateView> getAssessmentSubmittedCandidates(
            Long agencyId,
            Long recruitmentNotificationId) {
        requirePositiveId(agencyId, "Agency is required.");
        requirePositiveId(recruitmentNotificationId, "Request selection is required.");

        List<RecruitmentInterviewDetailEntity> candidates = interviewDetailRepository
                .findInternalAssessmentSubmittedCandidatesByAgencyAndNotification(agencyId, recruitmentNotificationId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, RecruitmentAssessmentFeedbackEntity> assessmentMap = loadAssessmentMap(candidates);
        Map<Long, RecruitmentInternalLevelTwoScheduleEntity> scheduleMap = loadLevelTwoScheduleMap(candidates);
        Map<Long, String> interviewerNameMap = loadInterviewerNameMap(assessmentMap.values());

        return candidates.stream()
                .map(candidate -> toCandidateView(
                        candidate,
                        assessmentMap.get(candidate.getRecruitmentInterviewDetailId()),
                        scheduleMap.get(candidate.getRecruitmentInterviewDetailId()),
                        interviewerNameMap))
                .toList();
    }

    @Override
    public AgencyInternalAssessmentDetailView getAssessmentSubmittedCandidateDetail(
            Long agencyId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        requirePositiveId(agencyId, "Agency is required.");
        requirePositiveId(recruitmentNotificationId, "Request selection is required.");
        requirePositiveId(recruitmentInterviewDetailId, "Candidate is required.");

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findInternalAssessmentSubmittedCandidateByAgencyAndNotificationAndId(
                        agencyId,
                        recruitmentNotificationId,
                        recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal assessment candidate not found for this agency."));

        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository
                .findByCandidateForInternalVacancy(
                        candidate.getRecruitmentNotification().getInternalVacancyOpening().getInternalVacancyOpeningId(),
                        recruitmentInterviewDetailId)
                .orElse(null);
        RecruitmentInternalLevelTwoScheduleEntity levelTwoSchedule = levelTwoScheduleRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId)
                .orElse(null);
        Map<Long, String> interviewerNameMap = assessment == null
                ? Map.of()
                : loadInterviewerNameMap(List.of(assessment));

        return toDetailView(candidate, assessment, levelTwoSchedule, interviewerNameMap);
    }

    @Override
    @Transactional
    public void scheduleLevelTwoInterview(
            Long agencyId,
            Long agencyUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyCandidateInterviewScheduleInput scheduleInput) {
        requirePositiveId(agencyId, "Agency is required.");
        requirePositiveId(agencyUserId, "Agency user is required.");
        requirePositiveId(recruitmentNotificationId, "Request selection is required.");
        requirePositiveId(recruitmentInterviewDetailId, "Candidate is required.");
        validateScheduleInput(scheduleInput);

        RecruitmentInterviewDetailEntity candidate = interviewDetailRepository
                .findInternalAssessmentSubmittedCandidateByAgencyAndNotificationAndIdForUpdate(
                        agencyId,
                        recruitmentNotificationId,
                        recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal assessment candidate not found for this agency."));

        if (StringUtils.hasText(candidate.getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Level 2 interview cannot be updated.");
        }

        RecruitmentAssessmentFeedbackEntity assessment = assessmentFeedbackRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Assessment details are not available."));

        if (!RECOMMENDED_STATUS.equals(normalizeUpper(assessment.getRecommendationStatus()))) {
            throw new RecruitmentNotificationException(
                    "Only recommended candidates can be scheduled for Level 2 interview.");
        }

        RecruitmentInternalLevelTwoScheduleEntity schedule = levelTwoScheduleRepository
                .findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(recruitmentInterviewDetailId)
                .orElseGet(RecruitmentInternalLevelTwoScheduleEntity::new);

        if (schedule.getRecruitmentInternalLevelTwoScheduleId() == null) {
            schedule.setRecruitmentInterviewDetail(candidate);
        }

        schedule.setScheduledByUserId(agencyUserId);
        schedule.setScheduledAt(LocalDateTime.now());
        schedule.setInterviewDateTime(scheduleInput.getInterviewDateTime());
        schedule.setInterviewTimeSlot(normalizeText(scheduleInput.getInterviewTimeSlot()));
        schedule.setMeetingLink(normalizeText(scheduleInput.getInterviewLink()));
        schedule.setRemarks(normalizeText(scheduleInput.getInterviewRemarks()));
        schedule.setHrTimeChangeRequested(false);
        schedule.setHrTimeChangeReason(null);
        schedule.setHrTimeChangeRequestedAt(null);
        schedule.setHrTimeChangeRequestedByUserId(null);
        levelTwoScheduleRepository.save(schedule);
    }

    private AgencyInternalAssessmentProjectView toProjectView(
            List<RecruitmentInterviewDetailEntity> groupedCandidates,
            Map<Long, RecruitmentAssessmentFeedbackEntity> assessmentMap) {
        RecruitmentInterviewDetailEntity firstCandidate = groupedCandidates.get(0);
        long recommendedCount = groupedCandidates.stream()
                .map(candidate -> assessmentMap.get(candidate.getRecruitmentInterviewDetailId()))
                .filter(Objects::nonNull)
                .filter(assessment -> RECOMMENDED_STATUS.equals(normalizeUpper(assessment.getRecommendationStatus())))
                .count();

        LocalDateTime latestAssessmentSubmittedAt = groupedCandidates.stream()
                .map(candidate -> resolveAssessmentSubmittedAt(
                        candidate,
                        assessmentMap.get(candidate.getRecruitmentInterviewDetailId())))
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return AgencyInternalAssessmentProjectView.builder()
                .recruitmentNotificationId(firstCandidate.getRecruitmentNotification().getRecruitmentNotificationId())
                .requestId(firstCandidate.getRecruitmentNotification().getRequestId())
                .projectName(firstCandidate.getRecruitmentNotification().getProjectMst() != null
                        ? firstCandidate.getRecruitmentNotification().getProjectMst().getProjectName()
                        : null)
                .assessmentSubmittedCandidatesCount((long) groupedCandidates.size())
                .recommendedCandidatesCount(recommendedCount)
                .latestAssessmentSubmittedAt(latestAssessmentSubmittedAt)
                .build();
    }

    private AgencyInternalAssessmentCandidateView toCandidateView(
            RecruitmentInterviewDetailEntity candidate,
            RecruitmentAssessmentFeedbackEntity assessment,
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            Map<Long, String> interviewerNameMap) {
        boolean schedulingAllowed = isLevelTwoSchedulingAllowed(candidate, assessment);

        return AgencyInternalAssessmentCandidateView.builder()
                .recruitmentNotificationId(candidate.getRecruitmentNotification().getRecruitmentNotificationId())
                .requestId(candidate.getRecruitmentNotification().getRequestId())
                .projectName(candidate.getRecruitmentNotification().getProjectMst() != null
                        ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                        : null)
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .designationName(candidate.getDesignationVacancy() != null
                        && candidate.getDesignationVacancy().getDesignationMst() != null
                                ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                                : null)
                .levelCode(candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null)
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .resumeFilePath(candidate.getResumeFilePath())
                .interviewerGrade(assessment != null ? assessment.getInterviewerGrade() : null)
                .recommendationStatus(assessment != null ? normalizeUpper(assessment.getRecommendationStatus()) : null)
                .interviewAuthority(resolveInterviewAuthorityLabel(assessment, interviewerNameMap))
                .assessmentSubmittedAt(resolveAssessmentSubmittedAt(candidate, assessment))
                .levelTwoInterviewDateTime(schedule != null ? schedule.getInterviewDateTime() : null)
                .levelTwoInterviewTimeSlot(schedule != null ? schedule.getInterviewTimeSlot() : null)
                .levelTwoMeetingLink(schedule != null ? schedule.getMeetingLink() : null)
                .levelTwoScheduledAt(schedule != null ? schedule.getScheduledAt() : null)
                .levelTwoScheduled(schedule != null)
                .levelTwoSchedulingAllowed(schedulingAllowed)
                .levelTwoTimeChangeRequested(schedule != null
                        && Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()))
                .levelTwoTimeChangeReason(schedule != null ? schedule.getHrTimeChangeReason() : null)
                .levelTwoTimeChangeRequestedAt(schedule != null ? schedule.getHrTimeChangeRequestedAt() : null)
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .build();
    }

    private AgencyInternalAssessmentDetailView toDetailView(
            RecruitmentInterviewDetailEntity candidate,
            RecruitmentAssessmentFeedbackEntity assessment,
            RecruitmentInternalLevelTwoScheduleEntity levelTwoSchedule,
            Map<Long, String> interviewerNameMap) {
        return AgencyInternalAssessmentDetailView.builder()
                .recruitmentNotificationId(candidate.getRecruitmentNotification().getRecruitmentNotificationId())
                .requestId(candidate.getRecruitmentNotification().getRequestId())
                .projectName(candidate.getRecruitmentNotification().getProjectMst() != null
                        ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                        : null)
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .designationName(candidate.getDesignationVacancy() != null
                        && candidate.getDesignationVacancy().getDesignationMst() != null
                                ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                                : null)
                .levelCode(candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null)
                .totalExperience(candidate.getTotalExperience())
                .relevantExperience(candidate.getRelevantExperience())
                .joiningTime(candidate.getJoiningTime())
                .resumeFilePath(candidate.getResumeFilePath())
                .initialInterviewDateTime(candidate.getInterviewDateTime())
                .initialInterviewTimeSlot(candidate.getInterviewTimeSlot())
                .initialInterviewLink(candidate.getInterviewLink())
                .assessment(toAssessmentView(assessment, interviewerNameMap))
                .levelTwoInterviewDateTime(levelTwoSchedule != null ? levelTwoSchedule.getInterviewDateTime() : null)
                .levelTwoInterviewTimeSlot(levelTwoSchedule != null ? levelTwoSchedule.getInterviewTimeSlot() : null)
                .levelTwoMeetingLink(levelTwoSchedule != null ? levelTwoSchedule.getMeetingLink() : null)
                .levelTwoRemarks(levelTwoSchedule != null ? levelTwoSchedule.getRemarks() : null)
                .levelTwoScheduledAt(levelTwoSchedule != null ? levelTwoSchedule.getScheduledAt() : null)
                .levelTwoScheduled(levelTwoSchedule != null)
                .levelTwoSchedulingAllowed(isLevelTwoSchedulingAllowed(candidate, assessment))
                .levelTwoTimeChangeRequested(levelTwoSchedule != null
                        && Boolean.TRUE.equals(levelTwoSchedule.getHrTimeChangeRequested()))
                .levelTwoTimeChangeReason(levelTwoSchedule != null ? levelTwoSchedule.getHrTimeChangeReason() : null)
                .levelTwoTimeChangeRequestedAt(
                        levelTwoSchedule != null ? levelTwoSchedule.getHrTimeChangeRequestedAt() : null)
                .build();
    }

    private Map<Long, RecruitmentAssessmentFeedbackEntity> loadAssessmentMap(
            List<RecruitmentInterviewDetailEntity> candidates) {
        List<Long> candidateIds = candidates.stream()
                .map(RecruitmentInterviewDetailEntity::getRecruitmentInterviewDetailId)
                .toList();
        return assessmentFeedbackRepository.findByRecruitmentInterviewDetailIds(candidateIds)
                .stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getRecruitmentInterviewDetail().getRecruitmentInterviewDetailId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, RecruitmentInternalLevelTwoScheduleEntity> loadLevelTwoScheduleMap(
            List<RecruitmentInterviewDetailEntity> candidates) {
        List<Long> candidateIds = candidates.stream()
                .map(RecruitmentInterviewDetailEntity::getRecruitmentInterviewDetailId)
                .toList();
        return levelTwoScheduleRepository.findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdIn(candidateIds)
                .stream()
                .collect(Collectors.toMap(
                        schedule -> schedule.getRecruitmentInterviewDetail().getRecruitmentInterviewDetailId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, String> loadInterviewerNameMap(Collection<RecruitmentAssessmentFeedbackEntity> assessments) {
        List<Long> interviewerUserIds = assessments.stream()
                .map(RecruitmentAssessmentFeedbackEntity::getInterviewerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (interviewerUserIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(interviewerUserIds)
                .stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::resolveUserLabel,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private DepartmentInterviewAssessmentView toAssessmentView(
            RecruitmentAssessmentFeedbackEntity assessment,
            Map<Long, String> interviewerNameMap) {
        if (assessment == null) {
            return null;
        }

        List<DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView> panelMemberViews =
                assessment.getPanelMembers() == null
                        ? List.of()
                        : assessment.getPanelMembers().stream()
                                .map(this::toPanelMemberView)
                                .toList();

        return DepartmentInterviewAssessmentView.builder()
                .recruitmentAssessmentFeedbackId(assessment.getRecruitmentAssessmentFeedbackId())
                .interviewAuthority(resolveInterviewAuthorityLabel(assessment, interviewerNameMap))
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
                .leadershipQualityMarks(assessment.getLeadershipQualityMarks())
                .relevantExperienceMarks(assessment.getRelevantExperienceMarks())
                .interviewerGrade(normalizeUpper(assessment.getInterviewerGrade()))
                .recommendationStatus(normalizeUpper(assessment.getRecommendationStatus()))
                .assessmentRemarks(assessment.getAssessmentRemarks())
                .finalRemarks(assessment.getFinalRemarks())
                .submittedAt(assessment.getCreatedDateTime())
                .panelMembers(panelMemberViews)
                .build();
    }

    private DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView toPanelMemberView(
            RecruitmentAssessmentPanelMemberEntity panelMember) {
        return DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView.builder()
                .panelMemberName(panelMember.getPanelMemberName())
                .panelMemberDesignation(panelMember.getPanelMemberDesignation())
                .build();
    }

    private LocalDateTime resolveAssessmentSubmittedAt(
            RecruitmentInterviewDetailEntity candidate,
            RecruitmentAssessmentFeedbackEntity assessment) {
        if (candidate.getAssessmentSubmittedAt() != null) {
            return candidate.getAssessmentSubmittedAt();
        }
        return assessment != null ? assessment.getCreatedDateTime() : null;
    }

    private boolean isLevelTwoSchedulingAllowed(
            RecruitmentInterviewDetailEntity candidate,
            RecruitmentAssessmentFeedbackEntity assessment) {
        return assessment != null
                && RECOMMENDED_STATUS.equals(normalizeUpper(assessment.getRecommendationStatus()))
                && !StringUtils.hasText(candidate.getFinalDecisionStatus());
    }

    private String resolveInterviewAuthorityLabel(
            RecruitmentAssessmentFeedbackEntity assessment,
            Map<Long, String> interviewerNameMap) {
        if (assessment == null) {
            return null;
        }
        if (assessment.getInterviewerUserId() != null) {
            String interviewerName = interviewerNameMap.get(assessment.getInterviewerUserId());
            if (StringUtils.hasText(interviewerName)) {
                return interviewerName;
            }
        }
        return normalizeText(assessment.getInterviewAuthority());
    }

    private String resolveUserLabel(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        return normalizeText(user.getEmail());
    }

    private void validateScheduleInput(AgencyCandidateInterviewScheduleInput scheduleInput) {
        if (scheduleInput == null) {
            throw new RecruitmentNotificationException("Level 2 interview details are required.");
        }
        if (scheduleInput.getInterviewDateTime() == null) {
            throw new RecruitmentNotificationException("Level 2 interview date is required.");
        }
        if (!StringUtils.hasText(scheduleInput.getInterviewTimeSlot())) {
            throw new RecruitmentNotificationException("Level 2 interview time slot is required.");
        }
        if (!StringUtils.hasText(scheduleInput.getInterviewLink())) {
            throw new RecruitmentNotificationException("Level 2 meeting link is required.");
        }
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
