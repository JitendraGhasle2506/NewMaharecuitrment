package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.dto.HrAgencyRankRowForm;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.service.HrAgencyRankMappingService;
import com.maharecruitment.gov.in.department.service.HrDepartmentRequestService;
import com.maharecruitment.gov.in.department.service.model.HrAgencyOptionView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingListRowView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingListView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingView;
import com.maharecruitment.gov.in.department.service.model.HrAssignedAgencyRankView;
import com.maharecruitment.gov.in.department.service.model.HrDepartmentApplicationReviewDetailView;
import com.maharecruitment.gov.in.department.service.model.HrRankReleaseRuleListView;
import com.maharecruitment.gov.in.department.service.model.HrRankReleaseRuleRowView;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyGlobalRankEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RankReleaseRuleEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyGlobalRankRepository;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RankReleaseRuleRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyRankAssignmentCommand;

@Service
@Transactional(readOnly = true)
public class HrAgencyRankMappingServiceImpl implements HrAgencyRankMappingService {

    private final HrDepartmentRequestService hrDepartmentRequestService;
    private final RecruitmentNotificationRepository recruitmentNotificationRepository;
    private final AgencyGlobalRankRepository agencyGlobalRankRepository;
    private final AgencyNotificationTrackingRepository agencyNotificationTrackingRepository;
    private final RankReleaseRuleRepository rankReleaseRuleRepository;
    private final RecruitmentNotificationRankAssignmentService recruitmentNotificationRankAssignmentService;
    private final AgencyMasterRepository agencyMasterRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public HrAgencyRankMappingServiceImpl(
            HrDepartmentRequestService hrDepartmentRequestService,
            RecruitmentNotificationRepository recruitmentNotificationRepository,
            AgencyGlobalRankRepository agencyGlobalRankRepository,
            AgencyNotificationTrackingRepository agencyNotificationTrackingRepository,
            RankReleaseRuleRepository rankReleaseRuleRepository,
            RecruitmentNotificationRankAssignmentService recruitmentNotificationRankAssignmentService,
            AgencyMasterRepository agencyMasterRepository,
            DepartmentProjectApplicationRepository departmentProjectApplicationRepository,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.hrDepartmentRequestService = hrDepartmentRequestService;
        this.recruitmentNotificationRepository = recruitmentNotificationRepository;
        this.agencyGlobalRankRepository = agencyGlobalRankRepository;
        this.agencyNotificationTrackingRepository = agencyNotificationTrackingRepository;
        this.rankReleaseRuleRepository = rankReleaseRuleRepository;
        this.recruitmentNotificationRankAssignmentService = recruitmentNotificationRankAssignmentService;
        this.agencyMasterRepository = agencyMasterRepository;
        this.departmentProjectApplicationRepository = departmentProjectApplicationRepository;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public HrAgencyRankMappingListView getAgencyRankMappingListView() {
        List<AgencyGlobalRankEntity> globalRanks = agencyGlobalRankRepository
                .findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();

        List<HrAgencyRankMappingListRowView> rows = globalRanks.stream()
                .map(globalRank -> HrAgencyRankMappingListRowView.builder()
                        .agencyId(globalRank.getAgency().getAgencyId())
                        .agencyName(globalRank.getAgency().getAgencyName())
                        .agencyEmail(globalRank.getAgency().getOfficialEmail())
                        .rankNumber(globalRank.getRankNumber())
                        .assignedDate(globalRank.getAssignedDate())
                        .releaseStatusCode("GLOBAL")
                        .releaseStatusLabel("Global rank")
                        .applicationContextAvailable(false)
                        .build())
                .toList();

        return HrAgencyRankMappingListView.builder()
                .rankMappings(rows)
                .build();
    }

    @Override
    public HrAgencyRankMappingListView getRankReleaseOverviewListView() {
        List<AgencyGlobalRankEntity> globalRanks = agencyGlobalRankRepository
                .findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();
        if (globalRanks.isEmpty()) {
            return HrAgencyRankMappingListView.builder()
                    .rankMappings(List.of())
                    .build();
        }

        List<RecruitmentNotificationEntity> notifications = recruitmentNotificationRepository.findAll().stream()
                .sorted(Comparator.comparing(RecruitmentNotificationEntity::getRecruitmentNotificationId).reversed())
                .toList();
        if (notifications.isEmpty()) {
            return HrAgencyRankMappingListView.builder()
                    .rankMappings(List.of())
                    .build();
        }

        Map<Long, DepartmentProjectApplicationEntity> applicationById = loadApplicationById(notifications);
        Map<Long, String> departmentNameById = loadDepartmentNameById(applicationById.values());
        Map<Long, String> subDepartmentNameById = loadSubDepartmentNameById(applicationById.values());
        List<Integer> sortedGlobalRanks = buildSortedGlobalRanks(globalRanks);
        Map<Integer, Integer> delayByRank = loadDelayByRank();

        Map<String, AgencyNotificationTrackingEntity> trackingByNotificationAndAgency = new HashMap<>();
        Map<String, LocalDateTime> firstReleaseByNotificationAndRank = new HashMap<>();
        Map<Long, Boolean> responseReceivedByNotification = new HashMap<>();
        loadTrackingMetadata(
                notifications,
                trackingByNotificationAndAgency,
                firstReleaseByNotificationAndRank,
                responseReceivedByNotification);

        LocalDateTime now = LocalDateTime.now();
        List<HrAgencyRankMappingListRowView> rows = new ArrayList<>();
        for (RecruitmentNotificationEntity notification : notifications) {
            for (AgencyGlobalRankEntity globalRank : globalRanks) {
                rows.add(toRankReleaseOverviewRow(
                        notification,
                        globalRank,
                        applicationById,
                        departmentNameById,
                        subDepartmentNameById,
                        sortedGlobalRanks,
                        delayByRank,
                        trackingByNotificationAndAgency,
                        firstReleaseByNotificationAndRank,
                        responseReceivedByNotification,
                        now));
            }
        }

        return HrAgencyRankMappingListView.builder()
                .rankMappings(rows)
                .build();
    }

    @Override
    public HrRankReleaseRuleListView getRankReleaseRuleListView() {
        List<HrRankReleaseRuleRowView> rows = rankReleaseRuleRepository.findAllByOrderByRankNumberAsc()
                .stream()
                .map(rule -> HrRankReleaseRuleRowView.builder()
                        .rankNumber(rule.getRankNumber())
                        .releaseAfterDays(rule.getReleaseAfterDays())
                        .delayFromPreviousRankDays(rule.getDelayFromPreviousRankDays())
                        .effectiveDelayDays(rule.getEffectiveReleaseDelayDays())
                        .effectiveFrom(rule.getEffectiveFrom())
                        .effectiveTo(rule.getEffectiveTo())
                        .active(Boolean.TRUE.equals(rule.getIsActive()))
                        .build())
                .toList();

        return HrRankReleaseRuleListView.builder()
                .rules(rows)
                .build();
    }

    @Override
    public HrAgencyRankMappingView getGlobalRankMappingView() {
        List<AgencyGlobalRankEntity> assignedRanks = agencyGlobalRankRepository
                .findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();

        List<HrAgencyOptionView> agencyOptions = buildAgencyOptions(assignedRanks);
        List<HrAssignedAgencyRankView> assignedAgencyRanks = assignedRanks.stream()
                .map(rankEntity -> HrAssignedAgencyRankView.builder()
                        .agencyId(rankEntity.getAgency().getAgencyId())
                        .agencyName(rankEntity.getAgency().getAgencyName())
                        .rankNumber(rankEntity.getRankNumber())
                        .build())
                .toList();

        return HrAgencyRankMappingView.builder()
                .departmentName("All Departments")
                .subDepartmentName("All Sub-Departments")
                .requestId("GLOBAL")
                .projectName("All Recruitment Notifications")
                .recruitmentNotificationAvailable(true)
                .agencyOptions(agencyOptions)
                .assignedAgencyRanks(assignedAgencyRanks)
                .build();
    }

    @Override
    public HrAgencyRankMappingView getRankMappingView(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId) {
        HrDepartmentApplicationReviewDetailView applicationReviewDetail = hrDepartmentRequestService.getApplicationReviewDetail(
                departmentId,
                subDepartmentId,
                applicationId);

        RecruitmentNotificationEntity recruitmentNotification = resolveRecruitmentNotification(
                applicationReviewDetail.getRequestId());

        List<AgencyGlobalRankEntity> assignedRanks = agencyGlobalRankRepository
                .findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();

        List<HrAgencyOptionView> agencyOptions = buildAgencyOptions(assignedRanks);
        List<HrAssignedAgencyRankView> assignedAgencyRanks = assignedRanks.stream()
                .map(rankEntity -> HrAssignedAgencyRankView.builder()
                        .agencyId(rankEntity.getAgency().getAgencyId())
                        .agencyName(rankEntity.getAgency().getAgencyName())
                        .rankNumber(rankEntity.getRankNumber())
                        .build())
                .toList();

        return HrAgencyRankMappingView.builder()
                .departmentId(applicationReviewDetail.getDepartmentId())
                .departmentName(applicationReviewDetail.getDepartmentName())
                .subDepartmentId(applicationReviewDetail.getSubDepartmentId())
                .subDepartmentName(applicationReviewDetail.getSubDepartmentName())
                .departmentProjectApplicationId(applicationReviewDetail.getDepartmentProjectApplicationId())
                .requestId(applicationReviewDetail.getRequestId())
                .projectName(applicationReviewDetail.getProjectName())
                .recruitmentNotificationAvailable(recruitmentNotification != null)
                .recruitmentNotificationId(
                        recruitmentNotification != null ? recruitmentNotification.getRecruitmentNotificationId() : null)
                .recruitmentNotificationStatus(
                        recruitmentNotification != null ? recruitmentNotification.getStatus() : null)
                .agencyOptions(agencyOptions)
                .assignedAgencyRanks(assignedAgencyRanks)
                .build();
    }

    @Override
    @Transactional
    public void assignGlobalAgencyRanks(List<HrAgencyRankRowForm> rankRows) {
        List<HrAgencyRankRowForm> normalizedRows = normalizeRows(rankRows);
        validateRows(normalizedRows);

        List<AgencyRankAssignmentCommand> assignmentCommands = normalizedRows.stream()
                .map(row -> AgencyRankAssignmentCommand.builder()
                        .agencyId(row.getAgencyId())
                        .rankNumber(row.getRankNumber())
                        .build())
                .toList();

        try {
            recruitmentNotificationRankAssignmentService.assignAgencyRanks(
                    null,
                    assignmentCommands);
        } catch (RecruitmentNotificationException ex) {
            throw new DepartmentApplicationException(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void assignAgencyRanks(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            List<HrAgencyRankRowForm> rankRows) {
        HrDepartmentApplicationReviewDetailView applicationReviewDetail = hrDepartmentRequestService.getApplicationReviewDetail(
                departmentId,
                subDepartmentId,
                applicationId);

        RecruitmentNotificationEntity recruitmentNotification = resolveRecruitmentNotification(
                applicationReviewDetail.getRequestId());

        List<HrAgencyRankRowForm> normalizedRows = normalizeRows(rankRows);
        validateRows(normalizedRows);

        List<AgencyRankAssignmentCommand> assignmentCommands = normalizedRows.stream()
                .map(row -> AgencyRankAssignmentCommand.builder()
                        .agencyId(row.getAgencyId())
                        .rankNumber(row.getRankNumber())
                        .build())
                .toList();

        try {
            recruitmentNotificationRankAssignmentService.assignAgencyRanks(
                    recruitmentNotification != null ? recruitmentNotification.getRecruitmentNotificationId() : null,
                    assignmentCommands);
        } catch (RecruitmentNotificationException ex) {
            throw new DepartmentApplicationException(ex.getMessage());
        }
    }

    private RecruitmentNotificationEntity resolveRecruitmentNotification(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return recruitmentNotificationRepository.findByRequestIdIgnoreCase(requestId).orElse(null);
    }

    private List<HrAgencyOptionView> buildAgencyOptions(List<AgencyGlobalRankEntity> assignedRanks) {
        Map<Long, HrAgencyOptionView> optionsByAgencyId = new LinkedHashMap<>();

        agencyMasterRepository.findByStatusOrderByAgencyNameAsc(AgencyStatus.ACTIVE).forEach(agency -> optionsByAgencyId.put(
                agency.getAgencyId(),
                mapAgencyOption(agency)));

        for (AgencyGlobalRankEntity assignedRank : assignedRanks) {
            AgencyMaster agency = assignedRank.getAgency();
            optionsByAgencyId.putIfAbsent(
                    agency.getAgencyId(),
                    mapAgencyOption(agency));
        }

        return optionsByAgencyId.values().stream()
                .sorted(Comparator.comparing(HrAgencyOptionView::getAgencyName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private HrAgencyOptionView mapAgencyOption(AgencyMaster agency) {
        return HrAgencyOptionView.builder()
                .agencyId(agency.getAgencyId())
                .agencyName(agency.getAgencyName())
                .officialEmail(agency.getOfficialEmail())
                .build();
    }

    private List<HrAgencyRankRowForm> normalizeRows(List<HrAgencyRankRowForm> rankRows) {
        if (rankRows == null || rankRows.isEmpty()) {
            return List.of();
        }

        List<HrAgencyRankRowForm> normalizedRows = new ArrayList<>();
        for (HrAgencyRankRowForm row : rankRows) {
            if (row == null) {
                continue;
            }
            if (row.getAgencyId() == null && row.getRankNumber() == null) {
                continue;
            }

            HrAgencyRankRowForm normalizedRow = new HrAgencyRankRowForm();
            normalizedRow.setAgencyId(row.getAgencyId());
            normalizedRow.setRankNumber(row.getRankNumber());
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private void validateRows(List<HrAgencyRankRowForm> normalizedRows) {
        if (normalizedRows.isEmpty()) {
            throw new DepartmentApplicationException("Please add at least one agency rank mapping.");
        }

        Set<Long> uniqueAgencyIds = new LinkedHashSet<>();
        for (int index = 0; index < normalizedRows.size(); index++) {
            HrAgencyRankRowForm row = normalizedRows.get(index);
            int rowNumber = index + 1;

            if (row.getAgencyId() == null) {
                throw new DepartmentApplicationException("Please select agency in row " + rowNumber + ".");
            }
            if (row.getRankNumber() == null) {
                throw new DepartmentApplicationException("Please enter rank in row " + rowNumber + ".");
            }
            if (row.getRankNumber() < 1) {
                throw new DepartmentApplicationException("Rank must be at least 1 in row " + rowNumber + ".");
            }
            if (!uniqueAgencyIds.add(row.getAgencyId())) {
                throw new DepartmentApplicationException("Duplicate agency mapping is not allowed.");
            }
        }
    }

    private Map<Long, DepartmentProjectApplicationEntity> loadApplicationById(
            List<RecruitmentNotificationEntity> notifications) {
        Set<Long> applicationIds = notifications.stream()
                .map(RecruitmentNotificationEntity::getDepartmentProjectApplicationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        return departmentProjectApplicationRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(
                        DepartmentProjectApplicationEntity::getDepartmentProjectApplicationId,
                        application -> application,
                        (first, second) -> first));
    }

    private Map<Long, String> loadDepartmentNameById(Iterable<DepartmentProjectApplicationEntity> applications) {
        Set<Long> departmentIds = new LinkedHashSet<>();
        for (DepartmentProjectApplicationEntity application : applications) {
            if (application != null && application.getDepartmentId() != null) {
                departmentIds.add(application.getDepartmentId());
            }
        }

        if (departmentIds.isEmpty()) {
            return Map.of();
        }

        return departmentMstRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(
                        DepartmentMst::getDepartmentId,
                        DepartmentMst::getDepartmentName,
                        (first, second) -> first));
    }

    private Map<Long, String> loadSubDepartmentNameById(Iterable<DepartmentProjectApplicationEntity> applications) {
        Set<Long> subDepartmentIds = new LinkedHashSet<>();
        for (DepartmentProjectApplicationEntity application : applications) {
            if (application != null && application.getSubDepartmentId() != null) {
                subDepartmentIds.add(application.getSubDepartmentId());
            }
        }

        if (subDepartmentIds.isEmpty()) {
            return Map.of();
        }

        return subDepartmentRepository.findAllById(subDepartmentIds).stream()
                .collect(Collectors.toMap(
                        SubDepartment::getSubDeptId,
                        SubDepartment::getSubDeptName,
                        (first, second) -> first));
    }

    private HrAgencyRankMappingListRowView toRankReleaseOverviewRow(
            RecruitmentNotificationEntity notification,
            AgencyGlobalRankEntity globalRank,
            Map<Long, DepartmentProjectApplicationEntity> applicationById,
            Map<Long, String> departmentNameById,
            Map<Long, String> subDepartmentNameById,
            List<Integer> sortedGlobalRanks,
            Map<Integer, Integer> delayByRank,
            Map<String, AgencyNotificationTrackingEntity> trackingByNotificationAndAgency,
            Map<String, LocalDateTime> firstReleaseByNotificationAndRank,
            Map<Long, Boolean> responseReceivedByNotification,
            LocalDateTime now) {
        Long notificationId = notification.getRecruitmentNotificationId();
        Long applicationId = notification.getDepartmentProjectApplicationId();
        DepartmentProjectApplicationEntity application = applicationId != null
                ? applicationById.get(applicationId)
                : null;

        Long departmentId = application != null ? application.getDepartmentId() : null;
        Long subDepartmentId = application != null ? application.getSubDepartmentId() : null;

        String departmentName = resolveDepartmentName(departmentId, departmentNameById);
        String subDepartmentName = resolveSubDepartmentName(subDepartmentId, subDepartmentNameById);
        String projectName = resolveProjectName(notification, application);
        Long agencyId = globalRank.getAgency().getAgencyId();

        AgencyNotificationTrackingEntity tracking = trackingByNotificationAndAgency.get(
                trackingKey(notificationId, agencyId));

        Integer previousAssignedRank = resolvePreviousAssignedRank(
                globalRank.getRankNumber(),
                sortedGlobalRanks);

        Integer delayFromPreviousRankDays = previousAssignedRank == null
                ? 0
                : delayByRank.get(globalRank.getRankNumber());

        LocalDateTime eligibleOn = previousAssignedRank == null
                ? resolveRankOneEligibleOn(notification, globalRank)
                : null;

        LocalDateTime previousRankReleasedOn = null;
        if (previousAssignedRank != null) {
            previousRankReleasedOn = firstReleaseByNotificationAndRank.get(
                    notificationRankKey(notificationId, previousAssignedRank));
            if (previousRankReleasedOn != null && delayFromPreviousRankDays != null) {
                eligibleOn = previousRankReleasedOn.plusDays(delayFromPreviousRankDays);
            }
        }
        boolean delayRuleMissing = previousAssignedRank != null && delayFromPreviousRankDays == null;

        LocalDateTime releasedOn = tracking != null ? tracking.getNotifiedAt() : null;
        ReleaseStatusView releaseStatus = resolveReleaseStatus(
                tracking,
                notification,
                globalRank.getRankNumber(),
                previousAssignedRank,
                previousRankReleasedOn,
                eligibleOn,
                delayRuleMissing,
                responseReceivedByNotification,
                now);

        return HrAgencyRankMappingListRowView.builder()
                .recruitmentNotificationId(
                        notificationId)
                .requestId(notification.getRequestId())
                .projectName(projectName)
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentName)
                .departmentProjectApplicationId(applicationId)
                .agencyId(agencyId)
                .agencyName(globalRank.getAgency().getAgencyName())
                .agencyEmail(globalRank.getAgency().getOfficialEmail())
                .rankNumber(globalRank.getRankNumber())
                .assignedDate(globalRank.getAssignedDate())
                .delayFromPreviousRankDays(delayFromPreviousRankDays)
                .eligibleOn(eligibleOn)
                .releasedOn(releasedOn)
                .releaseStatusCode(releaseStatus.code())
                .releaseStatusLabel(releaseStatus.label())
                .applicationContextAvailable(departmentId != null && subDepartmentId != null && applicationId != null)
                .build();
    }

    private List<Integer> buildSortedGlobalRanks(List<AgencyGlobalRankEntity> globalRanks) {
        return globalRanks.stream()
                .map(AgencyGlobalRankEntity::getRankNumber)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<Integer, Integer> loadDelayByRank() {
        Map<Integer, Integer> delayByRank = new LinkedHashMap<>();
        for (RankReleaseRuleEntity rule : rankReleaseRuleRepository.findAllByOrderByRankNumberAsc()) {
            if (rule.getRankNumber() == null) {
                continue;
            }
            delayByRank.put(rule.getRankNumber(), rule.getEffectiveReleaseDelayDays());
        }
        return delayByRank;
    }

    private void loadTrackingMetadata(
            List<RecruitmentNotificationEntity> notifications,
            Map<String, AgencyNotificationTrackingEntity> trackingByNotificationAndAgency,
            Map<String, LocalDateTime> firstReleaseByNotificationAndRank,
            Map<Long, Boolean> responseReceivedByNotification) {
        Set<Long> notificationIds = notifications.stream()
                .map(RecruitmentNotificationEntity::getRecruitmentNotificationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (notificationIds.isEmpty()) {
            return;
        }

        List<AgencyNotificationTrackingEntity> trackingRows = agencyNotificationTrackingRepository
                .findByRecruitmentNotificationIds(notificationIds);
        for (AgencyNotificationTrackingEntity tracking : trackingRows) {
            if (tracking.getRecruitmentNotification() == null || tracking.getAgency() == null) {
                continue;
            }
            Long notificationId = tracking.getRecruitmentNotification().getRecruitmentNotificationId();
            Long agencyId = tracking.getAgency().getAgencyId();
            if (notificationId == null || agencyId == null) {
                continue;
            }

            trackingByNotificationAndAgency.put(
                    trackingKey(notificationId, agencyId),
                    tracking);

            if (tracking.getStatus() == AgencyNotificationTrackingStatus.RESPONDED) {
                responseReceivedByNotification.put(notificationId, true);
            }

            if (tracking.getReleasedRank() == null || tracking.getNotifiedAt() == null) {
                continue;
            }
            String notificationRankKey = notificationRankKey(notificationId, tracking.getReleasedRank());
            LocalDateTime currentFirstRelease = firstReleaseByNotificationAndRank.get(notificationRankKey);
            if (currentFirstRelease == null || tracking.getNotifiedAt().isBefore(currentFirstRelease)) {
                firstReleaseByNotificationAndRank.put(notificationRankKey, tracking.getNotifiedAt());
            }
        }
    }

    private Integer resolvePreviousAssignedRank(
            Integer rankNumber,
            List<Integer> sortedGlobalRanks) {
        if (rankNumber == null || sortedGlobalRanks == null || sortedGlobalRanks.isEmpty()) {
            return null;
        }

        Integer previousRank = null;
        for (Integer assignedRank : sortedGlobalRanks) {
            if (assignedRank == null) {
                continue;
            }
            if (assignedRank.equals(rankNumber)) {
                return previousRank;
            }
            previousRank = assignedRank;
        }
        return null;
    }

    private LocalDateTime resolveRankOneEligibleOn(
            RecruitmentNotificationEntity notification,
            AgencyGlobalRankEntity globalRank) {
        if (notification.getCreatedDateTime() != null) {
            return notification.getCreatedDateTime();
        }
        return globalRank.getAssignedDate();
    }

    private ReleaseStatusView resolveReleaseStatus(
            AgencyNotificationTrackingEntity tracking,
            RecruitmentNotificationEntity notification,
            Integer rankNumber,
            Integer previousAssignedRank,
            LocalDateTime previousRankReleasedOn,
            LocalDateTime eligibleOn,
            boolean delayRuleMissing,
            Map<Long, Boolean> responseReceivedByNotification,
            LocalDateTime now) {
        if (tracking != null) {
            String statusText = tracking.getStatus() != null
                    ? tracking.getStatus().name().replace('_', ' ')
                    : "RELEASED";
            return new ReleaseStatusView("RELEASED", "Released (" + statusText + ")");
        }

        if (notification.getStatus() == RecruitmentNotificationStatus.CLOSED) {
            return new ReleaseStatusView("CLOSED", "Notification closed");
        }

        Long notificationId = notification.getRecruitmentNotificationId();
        if (notificationId != null && Boolean.TRUE.equals(responseReceivedByNotification.get(notificationId))) {
            return new ReleaseStatusView("STOPPED", "Stopped after agency response");
        }

        if (delayRuleMissing) {
            return new ReleaseStatusView("RULE_MISSING", "Delay rule missing for rank " + rankNumber);
        }

        if (previousAssignedRank != null && previousRankReleasedOn == null) {
            return new ReleaseStatusView("WAITING_PREVIOUS", "Waiting for rank " + previousAssignedRank + " release");
        }

        if (eligibleOn != null && now.isBefore(eligibleOn)) {
            return new ReleaseStatusView("WAITING_DATE", "Waiting until eligible date");
        }

        return new ReleaseStatusView("ELIGIBLE", "Eligible for release");
    }

    private String trackingKey(Long notificationId, Long agencyId) {
        return notificationId + ":" + agencyId;
    }

    private String notificationRankKey(Long notificationId, Integer rankNumber) {
        return notificationId + ":" + rankNumber;
    }

    private static final class ReleaseStatusView {
        private final String code;
        private final String label;

        private ReleaseStatusView(String code, String label) {
            this.code = code;
            this.label = label;
        }

        private String code() {
            return code;
        }

        private String label() {
            return label;
        }
    }

    private String resolveProjectName(
            RecruitmentNotificationEntity notification,
            DepartmentProjectApplicationEntity application) {
        if (notification != null
                && notification.getProjectMst() != null
                && StringUtils.hasText(notification.getProjectMst().getProjectName())) {
            return notification.getProjectMst().getProjectName();
        }
        if (application != null && StringUtils.hasText(application.getProjectName())) {
            return application.getProjectName();
        }
        return "-";
    }

    private String resolveDepartmentName(Long departmentId, Map<Long, String> departmentNameById) {
        if (departmentId == null) {
            return "-";
        }
        return departmentNameById.getOrDefault(departmentId, "Department " + departmentId);
    }

    private String resolveSubDepartmentName(Long subDepartmentId, Map<Long, String> subDepartmentNameById) {
        if (subDepartmentId == null) {
            return "-";
        }
        return subDepartmentNameById.getOrDefault(subDepartmentId, "Sub-Department " + subDepartmentId);
    }
}
