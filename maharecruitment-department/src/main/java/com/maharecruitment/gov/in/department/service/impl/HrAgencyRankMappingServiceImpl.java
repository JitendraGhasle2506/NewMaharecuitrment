package com.maharecruitment.gov.in.department.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
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
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationAgencyRankEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationAgencyRankRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyRankAssignmentCommand;

@Service
@Transactional(readOnly = true)
public class HrAgencyRankMappingServiceImpl implements HrAgencyRankMappingService {

    private final HrDepartmentRequestService hrDepartmentRequestService;
    private final RecruitmentNotificationRepository recruitmentNotificationRepository;
    private final RecruitmentNotificationAgencyRankRepository recruitmentNotificationAgencyRankRepository;
    private final RecruitmentNotificationRankAssignmentService recruitmentNotificationRankAssignmentService;
    private final AgencyMasterRepository agencyMasterRepository;
    private final DepartmentProjectApplicationRepository departmentProjectApplicationRepository;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public HrAgencyRankMappingServiceImpl(
            HrDepartmentRequestService hrDepartmentRequestService,
            RecruitmentNotificationRepository recruitmentNotificationRepository,
            RecruitmentNotificationAgencyRankRepository recruitmentNotificationAgencyRankRepository,
            RecruitmentNotificationRankAssignmentService recruitmentNotificationRankAssignmentService,
            AgencyMasterRepository agencyMasterRepository,
            DepartmentProjectApplicationRepository departmentProjectApplicationRepository,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.hrDepartmentRequestService = hrDepartmentRequestService;
        this.recruitmentNotificationRepository = recruitmentNotificationRepository;
        this.recruitmentNotificationAgencyRankRepository = recruitmentNotificationAgencyRankRepository;
        this.recruitmentNotificationRankAssignmentService = recruitmentNotificationRankAssignmentService;
        this.agencyMasterRepository = agencyMasterRepository;
        this.departmentProjectApplicationRepository = departmentProjectApplicationRepository;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public HrAgencyRankMappingListView getAgencyRankMappingListView() {
        List<RecruitmentNotificationAgencyRankEntity> agencyRankMappings = recruitmentNotificationAgencyRankRepository
                .findAllWithNotificationAndAgency();

        if (agencyRankMappings.isEmpty()) {
            return HrAgencyRankMappingListView.builder()
                    .rankMappings(List.of())
                    .build();
        }

        Map<Long, DepartmentProjectApplicationEntity> applicationById = loadApplicationById(agencyRankMappings);
        Map<Long, String> departmentNameById = loadDepartmentNameById(applicationById.values());
        Map<Long, String> subDepartmentNameById = loadSubDepartmentNameById(applicationById.values());

        List<HrAgencyRankMappingListRowView> rows = agencyRankMappings.stream()
                .map(agencyRank -> toRankMappingListRow(
                        agencyRank,
                        applicationById,
                        departmentNameById,
                        subDepartmentNameById))
                .toList();

        return HrAgencyRankMappingListView.builder()
                .rankMappings(rows)
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

        List<RecruitmentNotificationAgencyRankEntity> assignedRanks = recruitmentNotification != null
                ? recruitmentNotificationAgencyRankRepository
                        .findByRecruitmentNotificationRecruitmentNotificationIdOrderByRankNumberAscAgencyAgencyIdAsc(
                                recruitmentNotification.getRecruitmentNotificationId())
                : List.of();

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
        if (recruitmentNotification == null) {
            throw new DepartmentApplicationException(
                    "Recruitment notification is not created yet for this request. Please complete auditor approval first.");
        }

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
                    recruitmentNotification.getRecruitmentNotificationId(),
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

    private List<HrAgencyOptionView> buildAgencyOptions(List<RecruitmentNotificationAgencyRankEntity> assignedRanks) {
        Map<Long, HrAgencyOptionView> optionsByAgencyId = new LinkedHashMap<>();

        agencyMasterRepository.findByStatusOrderByAgencyNameAsc(AgencyStatus.ACTIVE).forEach(agency -> optionsByAgencyId.put(
                agency.getAgencyId(),
                mapAgencyOption(agency)));

        for (RecruitmentNotificationAgencyRankEntity assignedRank : assignedRanks) {
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
            List<RecruitmentNotificationAgencyRankEntity> agencyRankMappings) {
        Set<Long> applicationIds = agencyRankMappings.stream()
                .map(RecruitmentNotificationAgencyRankEntity::getRecruitmentNotification)
                .filter(Objects::nonNull)
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

    private HrAgencyRankMappingListRowView toRankMappingListRow(
            RecruitmentNotificationAgencyRankEntity agencyRank,
            Map<Long, DepartmentProjectApplicationEntity> applicationById,
            Map<Long, String> departmentNameById,
            Map<Long, String> subDepartmentNameById) {
        RecruitmentNotificationEntity notification = agencyRank.getRecruitmentNotification();
        Long applicationId = notification != null ? notification.getDepartmentProjectApplicationId() : null;
        DepartmentProjectApplicationEntity application = applicationId != null
                ? applicationById.get(applicationId)
                : null;

        Long departmentId = application != null ? application.getDepartmentId() : null;
        Long subDepartmentId = application != null ? application.getSubDepartmentId() : null;

        String departmentName = resolveDepartmentName(departmentId, departmentNameById);
        String subDepartmentName = resolveSubDepartmentName(subDepartmentId, subDepartmentNameById);
        String projectName = resolveProjectName(notification, application);

        return HrAgencyRankMappingListRowView.builder()
                .recruitmentNotificationId(
                        notification != null ? notification.getRecruitmentNotificationId() : null)
                .requestId(notification != null ? notification.getRequestId() : null)
                .projectName(projectName)
                .departmentId(departmentId)
                .departmentName(departmentName)
                .subDepartmentId(subDepartmentId)
                .subDepartmentName(subDepartmentName)
                .departmentProjectApplicationId(applicationId)
                .agencyId(agencyRank.getAgency().getAgencyId())
                .agencyName(agencyRank.getAgency().getAgencyName())
                .agencyEmail(agencyRank.getAgency().getOfficialEmail())
                .rankNumber(agencyRank.getRankNumber())
                .assignedDate(agencyRank.getAssignedDate())
                .applicationContextAvailable(departmentId != null && subDepartmentId != null && applicationId != null)
                .build();
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
