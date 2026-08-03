package com.maharecruitment.gov.in.web.service.hr.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingAuditLogEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingAuditLogRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeLocationMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingAuditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEditView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationMappingEmployeeView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeLocationOptionView;

@Service
@Transactional(readOnly = true)
public class EmployeeLocationMappingPageServiceImpl implements EmployeeLocationMappingPageService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeLocationMappingPageServiceImpl.class);
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String AUDIT_ACTION_UPDATED = "UPDATED";

    private final EmployeeRepository employeeRepository;
    private final LocationMasterRepository locationMasterRepository;
    private final EmployeeLocationMappingRepository employeeLocationMappingRepository;
    private final EmployeeLocationMappingAuditLogRepository auditLogRepository;

    public EmployeeLocationMappingPageServiceImpl(
            EmployeeRepository employeeRepository,
            LocationMasterRepository locationMasterRepository,
            EmployeeLocationMappingRepository employeeLocationMappingRepository,
            EmployeeLocationMappingAuditLogRepository auditLogRepository) {
        this.employeeRepository = employeeRepository;
        this.locationMasterRepository = locationMasterRepository;
        this.employeeLocationMappingRepository = employeeLocationMappingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Page<EmployeeLocationMappingEmployeeView> searchEmployees(
            String recruitmentType,
            String searchText,
            Pageable pageable) {
        String normalizedRecruitmentType = normalizeRecruitmentType(recruitmentType);
        String searchPattern = buildSearchPattern(searchText);

        Page<EmployeeEntity> employeePage = employeeRepository.findActiveOnboardedForLocationMapping(
                normalizedRecruitmentType,
                searchPattern,
                pageable);
        Map<Long, List<EmployeeLocationOptionView>> locationMap = loadMappedLocationViews(employeePage.getContent());

        List<EmployeeLocationMappingEmployeeView> content = employeePage.getContent().stream()
                .map(employee -> toEmployeeView(employee, locationMap.getOrDefault(employee.getEmployeeId(), List.of())))
                .toList();
        return new PageImpl<>(content, pageable, employeePage.getTotalElements());
    }

    @Override
    public EmployeeLocationMappingEditView loadMapping(Long employeeId) {
        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        List<EmployeeLocationMappingEntity> mappings = employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(employee.getEmployeeId());
        List<EmployeeLocationOptionView> selectedLocations = mappings.stream()
                .map(mapping -> toLocationOptionView(
                        mapping.getLocation(),
                        Boolean.TRUE.equals(mapping.getPrimaryLocation())))
                .toList();

        Set<Long> selectedIds = selectedLocations.stream()
                .map(EmployeeLocationOptionView::locationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<EmployeeLocationOptionView> availableLocations = locationMasterRepository
                .findByActiveFlagIgnoreCaseOrderByLocationNameAsc("Y")
                .stream()
                .filter(location -> location.getLocationId() != null)
                .map(location -> toLocationOptionView(location, false))
                .collect(Collectors.toCollection(ArrayList::new));
        selectedLocations.stream()
                .filter(location -> !location.active() && selectedIds.contains(location.locationId()))
                .forEach(availableLocations::add);

        List<EmployeeLocationMappingAuditView> auditLogs = auditLogRepository
                .findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(employee.getEmployeeId())
                .stream()
                .map(this::toAuditView)
                .toList();

        return new EmployeeLocationMappingEditView(
                toEmployeeView(employee, selectedLocations),
                availableLocations,
                selectedLocations,
                auditLogs);
    }

    @Override
    @Transactional
    public boolean updateMapping(
            Long employeeId,
            List<Long> selectedLocationIds,
            Long primaryLocationId,
            String actorLoginId) {
        if (primaryLocationId == null) {
            throw new RecruitmentNotificationException("A primary location must be designated.");
        }

        EmployeeEntity employee = loadEligibleEmployee(employeeId);
        List<LocationMaster> selectedLocations = resolveSelectedActiveLocations(selectedLocationIds);

        boolean primaryInSelected = selectedLocations.stream()
                .anyMatch(loc -> primaryLocationId.equals(loc.getLocationId()));
        if (!primaryInSelected) {
            throw new RecruitmentNotificationException(
                    "The designated primary location must be included in the selected locations.");
        }

        List<EmployeeLocationMappingEntity> existingMappings = employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(employee.getEmployeeId());

        Set<Long> existingLocationIds = existingMappings.stream()
                .map(mapping -> mapping.getLocation().getLocationId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> requestedLocationIds = selectedLocations.stream()
                .map(LocationMaster::getLocationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Long existingPrimaryLocationId = existingMappings.stream()
                .filter(m -> Boolean.TRUE.equals(m.getPrimaryLocation()))
                .map(m -> m.getLocation().getLocationId())
                .findFirst()
                .orElse(null);

        boolean locationSetChanged = !existingLocationIds.equals(requestedLocationIds);
        boolean primaryChanged = !Objects.equals(existingPrimaryLocationId, primaryLocationId);

        if (!locationSetChanged && !primaryChanged) {
            log.info(
                    "Employee location mapping unchanged. employeeId={}, actorLoginId={}, locationIds={}",
                    employee.getEmployeeId(),
                    actorLoginId,
                    requestedLocationIds);
            return false;
        }

        // Clear the old primary in the database before assigning the replacement so
        // PostgreSQL's partial unique index is never violated during a switch.
        employeeLocationMappingRepository.clearPrimaryForEmployee(employee.getEmployeeId());
        existingMappings.forEach(m -> m.setPrimaryLocation(false));

        Set<Long> removedLocationIds = new LinkedHashSet<>(existingLocationIds);
        removedLocationIds.removeAll(requestedLocationIds);

        Set<Long> addedLocationIds = new LinkedHashSet<>(requestedLocationIds);
        addedLocationIds.removeAll(existingLocationIds);

        List<EmployeeLocationMappingEntity> mappingsToRemove = existingMappings.stream()
                .filter(m -> removedLocationIds.contains(m.getLocation().getLocationId()))
                .toList();
        if (!mappingsToRemove.isEmpty()) {
            employeeLocationMappingRepository.deleteAll(mappingsToRemove);
        }

        Map<Long, LocationMaster> selectedLocationById = selectedLocations.stream()
                .collect(Collectors.toMap(LocationMaster::getLocationId, l -> l));
        List<EmployeeLocationMappingEntity> newMappings = addedLocationIds.stream()
                .map(selectedLocationById::get)
                .map(location -> {
                    EmployeeLocationMappingEntity m = new EmployeeLocationMappingEntity();
                    m.setEmployee(employee);
                    m.setLocation(location);
                    m.setPrimaryLocation(false);
                    return m;
                })
                .toList();
        if (!newMappings.isEmpty()) {
            employeeLocationMappingRepository.saveAll(newMappings);
        }
        employeeLocationMappingRepository.flush();

        EmployeeLocationMappingEntity primaryMapping = java.util.stream.Stream
                .concat(existingMappings.stream(), newMappings.stream())
                .filter(m -> primaryLocationId.equals(m.getLocation().getLocationId()))
                .findFirst()
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "The designated primary location could not be saved."));
        primaryMapping.setPrimaryLocation(true);
        employeeLocationMappingRepository.save(primaryMapping);
        employeeLocationMappingRepository.flush();

        saveAuditLog(
                employee,
                actorLoginId,
                existingLocationIds,
                requestedLocationIds,
                addedLocationIds,
                removedLocationIds,
                existingPrimaryLocationId,
                primaryLocationId,
                selectedLocations,
                existingMappings);

        log.info(
                "Employee location mapping updated. employeeId={}, actorLoginId={}, added={}, removed={}, primaryLocationId={}, final={}",
                employee.getEmployeeId(),
                actorLoginId,
                addedLocationIds,
                removedLocationIds,
                primaryLocationId,
                requestedLocationIds);
        return true;
    }

    private EmployeeEntity loadEligibleEmployee(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Valid employee id is required.");
        }
        EmployeeEntity employee = employeeRepository.findDetailedByEmployeeId(employeeId)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found."));
        if (!ACTIVE_STATUS.equalsIgnoreCase(employee.getStatus())) {
            throw new RecruitmentNotificationException("Location can be mapped only for active onboarded employees.");
        }
        if (!StringUtils.hasText(employee.getEmployeeCode())
                || "PENDING".equalsIgnoreCase(employee.getEmployeeCode().trim())
                || employee.getEmployeeCode().trim().toUpperCase().startsWith("TMP-")) {
            throw new RecruitmentNotificationException("Employee code is not finalized yet.");
        }
        return employee;
    }

    private List<LocationMaster> resolveSelectedActiveLocations(List<Long> selectedLocationIds) {
        Set<Long> uniqueLocationIds = new LinkedHashSet<>();
        if (selectedLocationIds != null) {
            selectedLocationIds.stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(uniqueLocationIds::add);
        }
        if (uniqueLocationIds.isEmpty()) {
            throw new RecruitmentNotificationException("Select at least one employee location.");
        }

        List<LocationMaster> locations = locationMasterRepository.findAllById(uniqueLocationIds);
        if (locations.size() != uniqueLocationIds.size()) {
            throw new RecruitmentNotificationException("One or more selected locations are invalid.");
        }

        List<LocationMaster> inactiveLocations = locations.stream()
                .filter(location -> !"Y".equalsIgnoreCase(location.getActiveFlag()))
                .toList();
        if (!inactiveLocations.isEmpty()) {
            throw new RecruitmentNotificationException("Inactive locations cannot be mapped to an employee.");
        }

        Map<Long, LocationMaster> locationById = locations.stream()
                .collect(Collectors.toMap(LocationMaster::getLocationId, location -> location));
        return uniqueLocationIds.stream()
                .map(locationById::get)
                .toList();
    }

    private Map<Long, List<EmployeeLocationOptionView>> loadMappedLocationViews(List<EmployeeEntity> employees) {
        List<Long> employeeIds = employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<EmployeeLocationOptionView>> mappedLocations = new HashMap<>();
        employeeLocationMappingRepository
                .findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAscPrimaryLocationDescLocationLocationNameAsc(
                        employeeIds)
                .forEach(mapping -> mappedLocations
                        .computeIfAbsent(mapping.getEmployee().getEmployeeId(), id -> new ArrayList<>())
                        .add(toLocationOptionView(
                                mapping.getLocation(),
                                Boolean.TRUE.equals(mapping.getPrimaryLocation()))));
        mappedLocations.values().forEach(locations -> locations.sort(
                Comparator.comparing(EmployeeLocationOptionView::primary)
                        .reversed()
                        .thenComparing(
                                EmployeeLocationOptionView::displayName,
                                String.CASE_INSENSITIVE_ORDER)));
        return mappedLocations;
    }

    private void saveAuditLog(
            EmployeeEntity employee,
            String actorLoginId,
            Set<Long> previousLocationIds,
            Set<Long> newLocationIds,
            Set<Long> addedLocationIds,
            Set<Long> removedLocationIds,
            Long previousPrimaryLocationId,
            Long newPrimaryLocationId,
            List<LocationMaster> selectedLocations,
            List<EmployeeLocationMappingEntity> existingMappings) {
        EmployeeLocationMappingAuditLogEntity auditLog = new EmployeeLocationMappingAuditLogEntity();
        auditLog.setEmployee(employee);
        auditLog.setActorLoginId(StringUtils.hasText(actorLoginId) ? actorLoginId.trim() : "SYSTEM");
        auditLog.setActionType(AUDIT_ACTION_UPDATED);
        auditLog.setPreviousLocationIds(joinIds(previousLocationIds));
        auditLog.setNewLocationIds(joinIds(newLocationIds));
        auditLog.setAddedLocationIds(joinIds(addedLocationIds));
        auditLog.setRemovedLocationIds(joinIds(removedLocationIds));
        auditLog.setSummary("Employee location mapping updated");
        auditLog.setDetails(buildAuditDetails(
                addedLocationIds,
                removedLocationIds,
                previousPrimaryLocationId,
                newPrimaryLocationId,
                selectedLocations,
                existingMappings));
        auditLogRepository.save(auditLog);
    }

    private String buildAuditDetails(
            Set<Long> addedLocationIds,
            Set<Long> removedLocationIds,
            Long previousPrimaryLocationId,
            Long newPrimaryLocationId,
            List<LocationMaster> selectedLocations,
            List<EmployeeLocationMappingEntity> existingMappings) {
        Map<Long, String> locationNameById = new HashMap<>();
        selectedLocations.forEach(location -> locationNameById.put(
                location.getLocationId(),
                buildLocationDisplayName(location)));
        existingMappings.forEach(mapping -> locationNameById.putIfAbsent(
                mapping.getLocation().getLocationId(),
                buildLocationDisplayName(mapping.getLocation())));

        String added = addedLocationIds.isEmpty()
                ? "None"
                : addedLocationIds.stream().map(locationNameById::get).collect(Collectors.joining(", "));
        String removed = removedLocationIds.isEmpty()
                ? "None"
                : removedLocationIds.stream().map(locationNameById::get).collect(Collectors.joining(", "));

        boolean primaryChanged = !Objects.equals(previousPrimaryLocationId, newPrimaryLocationId);
        if (primaryChanged) {
            String prevName = previousPrimaryLocationId != null
                    ? locationNameById.getOrDefault(previousPrimaryLocationId, "id:" + previousPrimaryLocationId)
                    : "None";
            String newName = newPrimaryLocationId != null
                    ? locationNameById.getOrDefault(newPrimaryLocationId, "id:" + newPrimaryLocationId)
                    : "None";
            return "Added: " + added + " | Removed: " + removed
                    + " | Primary changed from: " + prevName + " to: " + newName;
        }
        return "Added: " + added + " | Removed: " + removed;
    }

    private String joinIds(Set<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private EmployeeLocationMappingEmployeeView toEmployeeView(
            EmployeeEntity employee,
            List<EmployeeLocationOptionView> locations) {
        String departmentName = employee.getDepartmentRegistration() == null
                ? "-"
                : defaultIfBlank(employee.getDepartmentRegistration().getDepartmentName(), "-");
        String designationName = employee.getDesignation() == null
                ? "-"
                : defaultIfBlank(employee.getDesignation().getDesignationName(), "-");
        String projectName = "-";
        if (employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getInterviewDetail() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null) {
            projectName = defaultIfBlank(
                    employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                            .getProjectMst().getProjectName(),
                    "-");
        }

        return new EmployeeLocationMappingEmployeeView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getMobile(),
                designationName,
                departmentName,
                projectName,
                employee.getRecruitmentType(),
                employee.getJoiningDate(),
                employee.getOnboardingDate(),
                List.copyOf(locations));
    }

    private EmployeeLocationOptionView toLocationOptionView(LocationMaster location, boolean primary) {
        return new EmployeeLocationOptionView(
                location.getLocationId(),
                defaultIfBlank(location.getOfficeName(), ""),
                defaultIfBlank(location.getLocationName(), "-"),
                location.getLatitude(),
                location.getLongitude(),
                "Y".equalsIgnoreCase(location.getActiveFlag()),
                buildLocationDisplayName(location),
                primary);
    }

    private EmployeeLocationMappingAuditView toAuditView(EmployeeLocationMappingAuditLogEntity entity) {
        return new EmployeeLocationMappingAuditView(
                entity.getActionType(),
                entity.getActorLoginId(),
                entity.getPreviousLocationIds(),
                entity.getNewLocationIds(),
                entity.getAddedLocationIds(),
                entity.getRemovedLocationIds(),
                entity.getSummary(),
                entity.getDetails(),
                entity.getOccurredAt());
    }

    private String buildLocationDisplayName(LocationMaster location) {
        String officeName = defaultIfBlank(location.getOfficeName(), "");
        String address = defaultIfBlank(location.getLocationName(), "-");
        return StringUtils.hasText(officeName) ? officeName + " - " + address : address;
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        if (!StringUtils.hasText(recruitmentType) || "ALL".equalsIgnoreCase(recruitmentType)) {
            return null;
        }
        String normalized = recruitmentType.trim().toUpperCase();
        return "INTERNAL".equals(normalized) || "EXTERNAL".equals(normalized) ? normalized : null;
    }

    private String buildSearchPattern(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        return "%" + searchText.trim().toUpperCase() + "%";
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
