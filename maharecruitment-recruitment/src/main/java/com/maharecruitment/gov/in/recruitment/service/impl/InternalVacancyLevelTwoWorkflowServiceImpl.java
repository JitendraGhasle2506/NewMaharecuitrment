package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyLevelTwoWorkflowService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewAssessmentView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoCandidateSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelMemberView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoPanelUserOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowDetailView;

@Service
@Transactional(readOnly = true)
public class InternalVacancyLevelTwoWorkflowServiceImpl implements InternalVacancyLevelTwoWorkflowService {

    private static final String RECOMMENDED_STATUS = "RECOMMENDED";
    private static final List<String> ALLOWED_PANEL_ROLE_NAMES = List.of("ROLE_STM", "ROLE_COO", "ROLE_HOD");
    private static final Map<String, String> ALLOWED_PANEL_ROLE_LABELS = Map.of(
            "ROLE_STM", "STM",
            "ROLE_COO", "COO",
            "ROLE_HOD", "HOD");
    private static final int MIN_PANEL_MEMBER_COUNT = 2;
    private static final int MAX_PANEL_MEMBER_COUNT = 5;

    private final RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository;
    private final RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository;
    private final UserRepository userRepository;

    public InternalVacancyLevelTwoWorkflowServiceImpl(
            RecruitmentInternalLevelTwoScheduleRepository levelTwoScheduleRepository,
            RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository,
            UserRepository userRepository) {
        this.levelTwoScheduleRepository = levelTwoScheduleRepository;
        this.assessmentFeedbackRepository = assessmentFeedbackRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<InternalVacancyLevelTwoCandidateSummaryView> getScheduledCandidatePage(String search, Pageable pageable) {
        String searchPattern = normalizeSearchPattern(search);
        return levelTwoScheduleRepository.findHrScheduledCandidatePage(searchPattern, pageable)
                .map(this::toSummaryView);
    }

    @Override
    public InternalVacancyLevelTwoWorkflowDetailView getWorkflowDetail(Long recruitmentInterviewDetailId) {
        RecruitmentInternalLevelTwoScheduleEntity schedule = loadDetailedSchedule(recruitmentInterviewDetailId);
        RecruitmentAssessmentFeedbackEntity assessment = loadAssessment(schedule);
        Map<Long, String> interviewerNameMap = assessment == null
                ? Map.of()
                : loadInterviewerNameMap(List.of(assessment));
        return toDetailView(schedule, assessment, interviewerNameMap);
    }

    @Override
    public List<InternalVacancyLevelTwoPanelUserOptionView> getEligiblePanelUsers() {
        return userRepository.findDistinctUsersByRoleNames(ALLOWED_PANEL_ROLE_NAMES).stream()
                .map(this::toEligiblePanelUserView)
                .sorted(Comparator
                        .comparing(
                                (InternalVacancyLevelTwoPanelUserOptionView user) -> normalizeSortValue(user.getName()))
                        .thenComparing(user -> normalizeSortValue(user.getEmail())))
                .toList();
    }

    @Override
    @Transactional
    public void assignInterviewPanel(
            Long recruitmentInterviewDetailId,
            String actorEmail,
            List<Long> panelUserIds) {
        RecruitmentInternalLevelTwoScheduleEntity schedule = loadScheduleForUpdate(recruitmentInterviewDetailId);
        User actor = resolveUser(actorEmail);
        List<User> resolvedPanelUsers = validateAndResolvePanelUsers(schedule, panelUserIds);

        List<RecruitmentInternalLevelTwoPanelMemberEntity> members = resolvedPanelUsers.stream()
                .map(this::buildPanelMemberEntity)
                .toList();

        schedule.replacePanelMembers(members);
        schedule.setPanelAssignedByUserId(actor.getId());
        schedule.setPanelAssignedAt(LocalDateTime.now());
        levelTwoScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void requestInterviewTimeChange(
            Long recruitmentInterviewDetailId,
            String actorEmail,
            String changeReason) {
        RecruitmentInternalLevelTwoScheduleEntity schedule = loadScheduleForUpdate(recruitmentInterviewDetailId);
        User actor = resolveUser(actorEmail);

        String normalizedReason = normalizeText(changeReason);
        if (!StringUtils.hasText(normalizedReason)) {
            throw new RecruitmentNotificationException("Reason is required for interview time change request.");
        }
        if (StringUtils.hasText(schedule.getRecruitmentInterviewDetail().getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Interview change cannot be requested.");
        }

        schedule.setHrTimeChangeRequested(true);
        schedule.setHrTimeChangeReason(normalizedReason);
        schedule.setHrTimeChangeRequestedAt(LocalDateTime.now());
        schedule.setHrTimeChangeRequestedByUserId(actor.getId());
        levelTwoScheduleRepository.save(schedule);
    }

    private InternalVacancyLevelTwoCandidateSummaryView toSummaryView(
            RecruitmentInternalLevelTwoScheduleEntity schedule) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var notification = candidate.getRecruitmentNotification();
        var vacancy = candidate.getDesignationVacancy();

        return InternalVacancyLevelTwoCandidateSummaryView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .designationName(vacancy != null && vacancy.getDesignationMst() != null
                        ? vacancy.getDesignationMst().getDesignationName()
                        : null)
                .levelCode(vacancy != null ? vacancy.getLevelCode() : null)
                .recommendationStatus(RECOMMENDED_STATUS)
                .levelTwoInterviewDateTime(schedule.getInterviewDateTime())
                .levelTwoInterviewTimeSlot(schedule.getInterviewTimeSlot())
                .levelTwoScheduledAt(schedule.getScheduledAt())
                .panelAssigned(schedule.getPanelAssignedAt() != null)
                .panelAssignedAt(schedule.getPanelAssignedAt())
                .timeChangeRequested(Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()))
                .timeChangeRequestedAt(schedule.getHrTimeChangeRequestedAt())
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .build();
    }

    private InternalVacancyLevelTwoWorkflowDetailView toDetailView(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            RecruitmentAssessmentFeedbackEntity assessment,
            Map<Long, String> interviewerNameMap) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var notification = candidate.getRecruitmentNotification();
        var vacancy = candidate.getDesignationVacancy();

        return InternalVacancyLevelTwoWorkflowDetailView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .recruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId())
                .requestId(notification.getRequestId())
                .projectName(notification.getProjectMst() != null ? notification.getProjectMst().getProjectName() : null)
                .candidateName(candidate.getCandidateName())
                .candidateEmail(candidate.getCandidateEmail())
                .candidateMobile(candidate.getCandidateMobile())
                .candidateEducation(candidate.getCandidateEducation())
                .designationName(vacancy != null && vacancy.getDesignationMst() != null
                        ? vacancy.getDesignationMst().getDesignationName()
                        : null)
                .levelCode(vacancy != null ? vacancy.getLevelCode() : null)
                .joiningTime(candidate.getJoiningTime())
                .resumeFilePath(candidate.getResumeFilePath())
                .recommendationStatus(assessment != null ? normalizeUpper(assessment.getRecommendationStatus()) : null)
                .finalDecisionStatus(normalizeUpper(candidate.getFinalDecisionStatus()))
                .levelTwoInterviewDateTime(schedule.getInterviewDateTime())
                .levelTwoInterviewTimeSlot(schedule.getInterviewTimeSlot())
                .levelTwoMeetingLink(schedule.getMeetingLink())
                .levelTwoRemarks(schedule.getRemarks())
                .levelTwoScheduledAt(schedule.getScheduledAt())
                .timeChangeRequested(Boolean.TRUE.equals(schedule.getHrTimeChangeRequested()))
                .timeChangeReason(schedule.getHrTimeChangeReason())
                .timeChangeRequestedAt(schedule.getHrTimeChangeRequestedAt())
                .panelAssigned(schedule.getPanelAssignedAt() != null)
                .panelAssignedAt(schedule.getPanelAssignedAt())
                .assessment(toAssessmentView(assessment, interviewerNameMap))
                .panelMembers(schedule.getPanelMembers() == null
                        ? List.of()
                        : schedule.getPanelMembers().stream()
                                .map(this::toPanelMemberView)
                                .toList())
                .build();
    }

    private RecruitmentInternalLevelTwoScheduleEntity loadDetailedSchedule(Long recruitmentInterviewDetailId) {
        requirePositiveId(recruitmentInterviewDetailId, "Candidate is required.");
        return levelTwoScheduleRepository.findDetailedInternalScheduleByCandidateId(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Level 2 interview record not found."));
    }

    private RecruitmentInternalLevelTwoScheduleEntity loadScheduleForUpdate(Long recruitmentInterviewDetailId) {
        requirePositiveId(recruitmentInterviewDetailId, "Candidate is required.");
        RecruitmentInternalLevelTwoScheduleEntity schedule = levelTwoScheduleRepository
                .findDetailedInternalScheduleByCandidateId(recruitmentInterviewDetailId)
                .orElseThrow(() -> new RecruitmentNotificationException("Level 2 interview record not found."));
        if (StringUtils.hasText(schedule.getRecruitmentInterviewDetail().getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Round L2 workflow cannot be modified.");
        }
        return schedule;
    }

    private RecruitmentAssessmentFeedbackEntity loadAssessment(RecruitmentInternalLevelTwoScheduleEntity schedule) {
        var candidate = schedule.getRecruitmentInterviewDetail();
        var internalVacancyOpening = candidate.getRecruitmentNotification().getInternalVacancyOpening();
        if (internalVacancyOpening == null) {
            return null;
        }
        return assessmentFeedbackRepository.findByCandidateForInternalVacancy(
                internalVacancyOpening.getInternalVacancyOpeningId(),
                candidate.getRecruitmentInterviewDetailId()).orElse(null);
    }

    private Map<Long, String> loadInterviewerNameMap(List<RecruitmentAssessmentFeedbackEntity> assessments) {
        List<Long> userIds = assessments.stream()
                .map(RecruitmentAssessmentFeedbackEntity::getInterviewerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::resolveUserLabel,
                        (left, right) -> left));
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
                                .map(this::toAssessmentPanelMemberView)
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

    private DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView
            toAssessmentPanelMemberView(RecruitmentAssessmentPanelMemberEntity member) {
        return DepartmentInterviewAssessmentView.DepartmentInterviewAssessmentPanelMemberView.builder()
                .panelMemberName(member.getPanelMemberName())
                .panelMemberDesignation(member.getPanelMemberDesignation())
                .build();
    }

    private InternalVacancyLevelTwoPanelMemberView toPanelMemberView(
            RecruitmentInternalLevelTwoPanelMemberEntity member) {
        return InternalVacancyLevelTwoPanelMemberView.builder()
                .panelUserId(member.getPanelUserId())
                .panelMemberName(member.getPanelMemberName())
                .panelMemberDesignation(member.getPanelMemberDesignation())
                .build();
    }

    private InternalVacancyLevelTwoPanelUserOptionView toEligiblePanelUserView(User user) {
        List<String> roleLabels = resolveAllowedPanelRoleLabels(user);
        String displayLabel = resolveUserLabel(user);
        if (!roleLabels.isEmpty()) {
            displayLabel = displayLabel + " (" + String.join(", ", roleLabels) + ")";
        }

        return InternalVacancyLevelTwoPanelUserOptionView.builder()
                .userId(user.getId())
                .name(resolveUserLabel(user))
                .email(normalizeText(user.getEmail()))
                .mobileNo(normalizeText(user.getMobileNo()))
                .displayLabel(displayLabel)
                .roleLabelCsv(String.join(",", roleLabels))
                .roleLabels(roleLabels)
                .build();
    }

    private RecruitmentInternalLevelTwoPanelMemberEntity buildPanelMemberEntity(User user) {
        RecruitmentInternalLevelTwoPanelMemberEntity entity = new RecruitmentInternalLevelTwoPanelMemberEntity();
        entity.setPanelUserId(user.getId());
        entity.setPanelMemberName(resolveUserLabel(user));
        entity.setPanelMemberDesignation(resolvePanelRoleSummary(user));
        return entity;
    }

    private List<User> validateAndResolvePanelUsers(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            List<Long> panelUserIds) {
        List<Long> normalizedUserIds = panelUserIds == null
                ? List.of()
                : panelUserIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (normalizedUserIds.isEmpty()) {
            throw new RecruitmentNotificationException("At least two panel members are required.");
        }
        if (normalizedUserIds.size() < MIN_PANEL_MEMBER_COUNT) {
            throw new RecruitmentNotificationException("Minimum two panel members are required.");
        }
        if (normalizedUserIds.size() > MAX_PANEL_MEMBER_COUNT) {
            throw new RecruitmentNotificationException("Maximum five panel members are allowed.");
        }
        if (StringUtils.hasText(schedule.getRecruitmentInterviewDetail().getFinalDecisionStatus())) {
            throw new RecruitmentNotificationException(
                    "Final decision is already taken for this candidate. Panel cannot be updated.");
        }

        Map<Long, User> panelUsersById = userRepository.findAllWithRolesByIdIn(normalizedUserIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (panelUsersById.size() != normalizedUserIds.size()) {
            throw new RecruitmentNotificationException("One or more selected panel users were not found.");
        }

        return normalizedUserIds.stream()
                .map(userId -> {
                    User user = panelUsersById.get(userId);
                    if (user == null) {
                        throw new RecruitmentNotificationException("One or more selected panel users were not found.");
                    }
                    if (resolveAllowedPanelRoleLabels(user).isEmpty()) {
                        throw new RecruitmentNotificationException(
                                "Only STM, COO, and HOD users can be selected for the Round L2 panel.");
                    }
                    return user;
                })
                .toList();
    }

    private User resolveUser(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }
        return userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));
    }

    private String resolveInterviewAuthorityLabel(
            RecruitmentAssessmentFeedbackEntity assessment,
            Map<Long, String> interviewerNameMap) {
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

    private List<String> resolveAllowedPanelRoleLabels(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        List<String> normalizedRoleNames = user.getRoles().stream()
                .map(role -> normalizeUpper(role.getName()))
                .filter(Objects::nonNull)
                .toList();
        return ALLOWED_PANEL_ROLE_NAMES.stream()
                .filter(normalizedRoleNames::contains)
                .map(ALLOWED_PANEL_ROLE_LABELS::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolvePanelRoleSummary(User user) {
        List<String> roleLabels = resolveAllowedPanelRoleLabels(user);
        return roleLabels.isEmpty() ? null : String.join(", ", roleLabels);
    }

    private void requirePositiveId(Long value, String message) {
        if (value == null || value < 1) {
            throw new RecruitmentNotificationException(message);
        }
    }

    private String normalizeSearchPattern(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return "%" + search.trim().toUpperCase() + "%";
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String normalizeSortValue(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }
}
