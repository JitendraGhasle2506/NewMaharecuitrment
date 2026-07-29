package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingHodProjection;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
public class ReportingManagerServiceImpl implements ReportingManagerService {

    private static final Logger log = LoggerFactory.getLogger(ReportingManagerServiceImpl.class);
    private static final String TYPE_COO = "COO";
    private static final String TYPE_HOD = "HOD";
    private static final String TYPE_STM = "STM";
    private static final String TYPE_PM = "PM";
    private static final String TYPE_OTHER = "OTHER";
    private static final String ROLE_HOD = "ROLE_HOD";
    private static final String ROLE_COO = "ROLE_COO";
    private static final String ROLE_STM = "ROLE_STM";
    private static final String ROLE_PM = "ROLE_PM";
    private static final String ACTIVE = "ACTIVE";
    private static final String ACTIVE_FLAG_Y = "Y";
    private static final String INTERNAL = "INTERNAL";
    private static final Set<String> HOD_DESIGNATION_NAMES = Set.of(
            "HOD",
            "HEAD OF DEPARTMENT",
            "HEAD OF DEPARTMENT (HOD)");
    private static final Set<String> STM_DESIGNATION_NAMES = Set.of(
            "STM",
            "SENIOR TECHNICAL MANAGER",
            "SENIOR TECHNICAL MANAGER (STM)");
    private static final Set<String> PM_DESIGNATION_NAMES = Set.of(
            "PM",
            "PROJECT MANAGER",
            "PROJECT MANAGER (PM)");
    private static final String HOD_MANAGER_NAME_PATTERN = "%HEAD%OF%DEPARTMENT%";
    private static final String STM_MANAGER_NAME_PATTERN = "%SENIOR%TECHNICAL%MANAGER%";
    private static final String PM_MANAGER_NAME_PATTERN = "%PROJECT%MANAGER%";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PositionMasterRepository positionRepository;

    @Autowired
    private ProjectMstRepository projectRepository;

    @Autowired
    private EmployeeReportingMappingRepository mappingRepository;

    @Override
    public List<Map<String, Object>> getHodUsers() {
        Set<Long> hodUserIds = new LinkedHashSet<>(userRepository.findDistinctUserIdsByRoleName(ROLE_HOD));
        return toAuthorityOptions(hodUserIds, Set.of());
    }

    @Override
    public List<Map<String, Object>> getReportingAuthorities() {
        Set<Long> hodUserIds = new LinkedHashSet<>(userRepository.findDistinctUserIdsByRoleName(ROLE_HOD));
        Set<Long> cooUserIds = new LinkedHashSet<>(userRepository.findDistinctUserIdsByRoleName(ROLE_COO));
        Set<Long> authorityUserIds = new LinkedHashSet<>(cooUserIds);
        authorityUserIds.addAll(hodUserIds);
        return toAuthorityOptions(authorityUserIds, cooUserIds);
    }

    private List<Map<String, Object>> toAuthorityOptions(Set<Long> authorityUserIds, Set<Long> cooUserIds) {
        return userRepository.findAllById(authorityUserIds).stream()
                .sorted(Comparator
                        .comparing((User user) -> cooUserIds.contains(user.getId()) ? TYPE_COO : TYPE_HOD)
                        .thenComparing(user -> user.getName() == null ? "" : user.getName(),
                                String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    String authorityType = cooUserIds.contains(user.getId()) ? TYPE_COO : TYPE_HOD;
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", user.getId());
                    option.put("name", formatAuthorityName(user));
                    option.put("authorityType", authorityType);
                    return option;
                })
                .toList();
    }

    private String formatAuthorityName(User user) {
        String name = user.getName() == null ? "" : user.getName();
        return user.getEmail() == null || user.getEmail().isBlank()
                ? name
                : name + " (" + user.getEmail() + ")";
    }

    @Override
    public List<Map<String, Object>> getManagersByType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Manager type is required.");
        }

        String normalizedType = normalizeManagerType(type);
        List<EmployeeEntity> managers = getManagersForType(normalizedType);

        Map<Long, User> mappedHodsByEmployeeId = TYPE_PM.equals(normalizedType)
                ? getMappedHodsByEmployeeId(managers)
                : Map.of();
        Map<Long, EmployeeReportingMappingEntity> reportingMappingsByEmployeeId =
                getLatestReportingMappingsByEmployeeId(managers);
        
        return managers.stream()
                .map(e -> {
                    User mappedHod = mappedHodsByEmployeeId.get(e.getEmployeeId());
                    EmployeeReportingMappingEntity reportingMapping =
                            reportingMappingsByEmployeeId.get(e.getEmployeeId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getEmployeeId());
                    map.put("name", buildManagerDisplayName(e, mappedHod));
                    if (mappedHod != null) {
                        map.put("mappedHodUserId", mappedHod.getId());
                        map.put("mappedHodName", mappedHod.getName());
                    }
                    map.put("mapped", reportingMapping != null);
                    if (reportingMapping != null) {
                        map.put("mappedAuthorityUserId", reportingMapping.getHodUserId());
                        map.put("mappedManagerType", reportingMapping.getManagerType());
                    }
                    return map;
                }).collect(Collectors.toList());
    }

    private Map<Long, EmployeeReportingMappingEntity> getLatestReportingMappingsByEmployeeId(
            List<EmployeeEntity> employees) {
        Set<Long> employeeIds = employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, EmployeeReportingMappingEntity> mappingsByEmployeeId = new HashMap<>();
        for (EmployeeReportingMappingEntity mapping : mappingRepository.findByEmployeeIdIn(employeeIds)) {
            if (mapping == null || mapping.getEmployeeId() == null) {
                continue;
            }
            mappingsByEmployeeId.merge(
                    mapping.getEmployeeId(),
                    mapping,
                    (existing, current) -> isLaterMapping(current, existing) ? current : existing);
        }
        return mappingsByEmployeeId;
    }

    private boolean isLaterMapping(
            EmployeeReportingMappingEntity current,
            EmployeeReportingMappingEntity existing) {
        if (current.getMappingId() == null) {
            return false;
        }
        return existing.getMappingId() == null
                || current.getMappingId() > existing.getMappingId();
    }

    private List<EmployeeEntity> getManagersForType(String normalizedType) {
        return switch (normalizedType) {
            case TYPE_HOD -> getActiveManagers(ROLE_HOD, HOD_DESIGNATION_NAMES, HOD_MANAGER_NAME_PATTERN);
            case TYPE_STM -> getActiveManagers(ROLE_STM, STM_DESIGNATION_NAMES, STM_MANAGER_NAME_PATTERN);
            case TYPE_PM -> getActiveManagers(ROLE_PM, PM_DESIGNATION_NAMES, PM_MANAGER_NAME_PATTERN);
            case TYPE_OTHER -> employeeRepository.findActiveEmployeesNotMappedAsReportingManagers();
            default -> throw new IllegalArgumentException("Unsupported manager type: " + normalizedType);
        };
    }

    private String buildManagerDisplayName(EmployeeEntity employee, User mappedHod) {
        String displayName = employee.getFullName() + " (" + employee.getEmployeeCode() + ")";
        if (mappedHod != null) {
            displayName += " - Mapped HOD: " + mappedHod.getName();
        }
        return displayName;
    }

    private Map<Long, User> getMappedHodsByEmployeeId(List<EmployeeEntity> managers) {
        try {
            return loadMappedHodsByEmployeeId(managers);
        } catch (RuntimeException ex) {
            log.warn("Unable to enrich PM manager options with mapped HOD details.", ex);
            return Map.of();
        }
    }

    private Map<Long, User> loadMappedHodsByEmployeeId(List<EmployeeEntity> managers) {
        Set<Long> managerEmployeeIds = managers.stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (managerEmployeeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, EmployeeReportingHodProjection> latestMappingsByEmployeeId =
                latestMappingsByEmployeeId(mappingRepository.findHodReferencesByEmployeeIdIn(managerEmployeeIds));
        if (latestMappingsByEmployeeId.isEmpty()) {
            return Map.of();
        }

        Set<Long> hodUserIds = latestMappingsByEmployeeId.values().stream()
                .map(EmployeeReportingHodProjection::getHodUserId)
                .collect(Collectors.toSet());
        Map<Long, User> hodUsersById = userRepository.findAllById(hodUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, User> mappedHodsByEmployeeId = new HashMap<>();
        latestMappingsByEmployeeId.forEach((employeeId, mapping) -> {
            User hod = hodUsersById.get(mapping.getHodUserId());
            if (hod != null) {
                mappedHodsByEmployeeId.put(employeeId, hod);
            }
        });
        return mappedHodsByEmployeeId;
    }

    private Map<Long, EmployeeReportingHodProjection> latestMappingsByEmployeeId(
            List<EmployeeReportingHodProjection> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return Map.of();
        }

        Map<Long, EmployeeReportingHodProjection> latestMappingsByEmployeeId = new HashMap<>();
        for (EmployeeReportingHodProjection mapping : mappings) {
            if (mapping == null || mapping.getEmployeeId() == null || mapping.getHodUserId() == null) {
                continue;
            }
            latestMappingsByEmployeeId.merge(
                    mapping.getEmployeeId(),
                    mapping,
                    (existing, current) -> isAfter(current, existing) ? current : existing);
        }
        return latestMappingsByEmployeeId;
    }

    private boolean isAfter(EmployeeReportingHodProjection current, EmployeeReportingHodProjection existing) {
        Long currentId = current.getMappingId();
        Long existingId = existing.getMappingId();
        if (currentId == null) {
            return false;
        }
        if (existingId == null) {
            return true;
        }
        return currentId > existingId;
    }

    private List<EmployeeEntity> getActiveManagers(
            String roleName,
            Set<String> managerNames,
            String managerNamePattern) {
        List<EmployeeEntity> roleOrDesignationManagers =
                findActiveRoleOrDesignationManagers(roleName, managerNames, managerNamePattern);
        List<EmployeeEntity> positionManagers =
                findFilledActivePositionManagers(managerNames, managerNamePattern);

        Map<Long, EmployeeEntity> uniqueManagers = new HashMap<>();
        addManagers(uniqueManagers, roleOrDesignationManagers);
        addManagers(uniqueManagers, positionManagers);
        return uniqueManagers.values().stream()
                .sorted(Comparator
                        .<EmployeeEntity, String>comparing(
                                e -> e.getFullName() == null ? "" : e.getFullName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(EmployeeEntity::getEmployeeId))
                .toList();
    }

    private List<EmployeeEntity> findActiveRoleOrDesignationManagers(
            String roleName,
            Set<String> managerNames,
            String managerNamePattern) {
        try {
            return employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                    roleName, managerNames, managerNamePattern);
        } catch (RuntimeException ex) {
            log.warn("Unable to load managers from role/designation source for roleName={}.", roleName, ex);
            return List.of();
        }
    }

    private List<EmployeeEntity> findFilledActivePositionManagers(
            Set<String> managerNames,
            String managerNamePattern) {
        try {
            return positionRepository.findFilledActiveEmployeesByManagerNames(
                    managerNames,
                    managerNamePattern,
                    OrganizationRecordStatus.ACTIVE,
                    PositionStatus.FILLED,
                    ACTIVE);
        } catch (RuntimeException ex) {
            log.warn("Unable to load managers from organization position source.", ex);
            return List.of();
        }
    }

    private void addManagers(Map<Long, EmployeeEntity> uniqueManagers, List<EmployeeEntity> managers) {
        if (managers == null) {
            return;
        }
        for (EmployeeEntity manager : managers) {
            if (manager != null && manager.getEmployeeId() != null) {
                uniqueManagers.putIfAbsent(manager.getEmployeeId(), manager);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getProjects() {
        List<ProjectMst> projects = projectRepository.findByActiveFlagIgnoreCaseOrderByProjectNameAsc(ACTIVE_FLAG_Y);
        return projects.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getProjectId());
            map.put("name", p.getProjectName());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getInternalEmployees(
            Long includeEmployeeId,
            Long hodUserId,
            String managerType) {
        Long hodEmployeeId = null;
        if (TYPE_OTHER.equalsIgnoreCase(managerType)) {
            if (hodUserId == null) {
                throw new IllegalArgumentException(
                        "Please select a reporting authority before selecting Other Employees.");
            }
            requireReportingAuthority(hodUserId);
            hodEmployeeId = employeeRepository.findByUser_Id(hodUserId)
                    .map(EmployeeEntity::getEmployeeId)
                    .orElse(null);
        }

        Set<Long> mappedEmpIds = mappingRepository.findAll().stream()
                .map(EmployeeReportingMappingEntity::getEmployeeId)
                .collect(Collectors.toSet());
        Long excludedHodEmployeeId = hodEmployeeId;
        return employeeRepository
                .findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(INTERNAL, ACTIVE)
                .stream()
                .filter(e -> !mappedEmpIds.contains(e.getEmployeeId()) || e.getEmployeeId().equals(includeEmployeeId))
                .filter(e -> excludedHodEmployeeId == null || !excludedHodEmployeeId.equals(e.getEmployeeId()))
                .sorted(Comparator.comparing(
                        e -> e.getFullName() == null ? "" : e.getFullName(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getEmployeeId());
                    String designationName = (e.getDesignation() != null) ? e.getDesignation().getDesignationName() : "";
                    String displayName = e.getFullName() + " (" + e.getEmployeeCode() + ")";
                    if (!designationName.isEmpty()) {
                        displayName += " - " + designationName;
                    }
                    map.put("name", displayName);
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllMappings() {
        List<EmployeeReportingMappingEntity> mappings = mappingRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<EmployeeEntity> employees = employeeRepository.findAll();
        Map<Long, EmployeeEntity> empMap = employees.stream().collect(Collectors.toMap(EmployeeEntity::getEmployeeId, e -> e));
        
        List<User> users = userRepository.findAll();
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        List<ProjectMst> projects = projectRepository.findAll();
        Map<Long, ProjectMst> projMap = projects.stream().collect(Collectors.toMap(ProjectMst::getProjectId, p -> p));

        for(EmployeeReportingMappingEntity m : mappings) {
           Map<String, Object> map = new HashMap<>();
           map.put("mappingId", m.getMappingId());
           map.put("employeeId", m.getEmployeeId());
           
           EmployeeEntity emp = empMap.get(m.getEmployeeId());
           String employeeName = "";
           if (emp != null) {
               String designationName = (emp.getDesignation() != null) ? emp.getDesignation().getDesignationName() : "";
               employeeName = emp.getFullName() + " (" + emp.getEmployeeCode() + ")";
               if (!designationName.isEmpty()) {
                   employeeName += " - " + designationName;
               }
           }
           map.put("employeeName", employeeName);
           
           map.put("projectId", m.getProjectId());
           ProjectMst proj = projMap.get(m.getProjectId());
           map.put("projectName", proj != null ? proj.getProjectName() : "");

           map.put("managerType", m.getManagerType());
           map.put("hodUserId", m.getHodUserId());
           User authority = userMap.get(m.getHodUserId());
           String authorityType = resolveAuthorityType(authority);
           String displayAuthorityType = authorityType != null ? authorityType : TYPE_HOD;
           map.put("authorityType", displayAuthorityType);
           map.put("authorityName", authority != null ? authority.getName() : "");
           map.put("hodName", authority != null ? authority.getName() : "");

           map.put("managerEmployeeId", m.getManagerEmployeeId());
           if (m.getManagerEmployeeId() == null) {
               map.put("managerName", authority != null
                       ? "Directly Reports to " + displayAuthorityType + " - " + authority.getName()
                       : "Directly Reports to Reporting Authority");
           } else {
               EmployeeEntity mgr = empMap.get(m.getManagerEmployeeId());
               map.put("managerName", mgr != null
                       ? mgr.getFullName() + " (" + mgr.getEmployeeCode() + ")"
                       : "");
           }
           
           result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public void saveMapping(Long hodUserId, String managerType, Long managerEmployeeId, Long projectId, List<Long> employeeIds) {
        MappingRequest request = validateRequest(
                hodUserId, managerType, managerEmployeeId, projectId, employeeIds);

        List<EmployeeReportingMappingEntity> existingMappings =
                mappingRepository.findByEmployeeIdIn(request.employeeIds());
        if (!existingMappings.isEmpty()) {
            throw new IllegalStateException("Employee already has an active reporting mapping.");
        }

        List<EmployeeReportingMappingEntity> mappings = request.employeeIds().stream()
                .map(employeeId -> toEntity(new EmployeeReportingMappingEntity(), request, employeeId))
                .toList();
        mappingRepository.saveAll(mappings);
        log.info("Created {} reporting mapping(s) for authority userId={} and managerType={}",
                mappings.size(), request.hodUserId(), request.managerType());
    }

    @Override
    @Transactional
    public void updateMapping(
            Long mappingId,
            Long hodUserId,
            String managerType,
            Long managerEmployeeId,
            Long projectId,
            Long employeeId) {
        if (mappingId == null) {
            throw new IllegalArgumentException("Mapping ID is required for editing.");
        }
        MappingRequest request = validateRequest(
                hodUserId, managerType, managerEmployeeId, projectId, List.of(employeeId));
        EmployeeReportingMappingEntity mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("Reporting mapping was not found."));

        EmployeeReportingMappingEntity duplicate = mappingRepository.findByEmployeeId(employeeId);
        if (duplicate != null && !mappingId.equals(duplicate.getMappingId())) {
            throw new IllegalStateException("Employee already has an active reporting mapping.");
        }

        mappingRepository.save(toEntity(mapping, request, employeeId));
        log.info("Updated reporting mappingId={} for employeeId={}", mappingId, employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveDirectReportingUserId(EmployeeReportingMappingEntity mapping) {
        if (mapping == null) {
            return null;
        }
        if (mapping.getManagerEmployeeId() == null) {
            return mapping.getHodUserId();
        }
        return employeeRepository.findById(mapping.getManagerEmployeeId())
                .map(EmployeeEntity::getUser)
                .map(User::getId)
                .orElse(null);
    }

    private MappingRequest validateRequest(
            Long hodUserId,
            String managerType,
            Long managerEmployeeId,
            Long projectId,
            List<Long> employeeIds) {
        if (hodUserId == null) {
            throw new IllegalArgumentException("Reporting authority selection is required.");
        }
        User authority = requireReportingAuthority(hodUserId);

        String normalizedType = normalizeManagerType(managerType);
        String authorityType = resolveAuthorityType(authority);
        if (TYPE_HOD.equals(normalizedType) && !TYPE_COO.equals(authorityType)) {
            throw new IllegalArgumentException("HOD manager mapping is available only when COO is selected.");
        }
        Long normalizedManagerId = managerEmployeeId;
        Long normalizedProjectId = null;
        if (managerEmployeeId != null) {
            EmployeeEntity manager = employeeRepository.findById(managerEmployeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected manager was not found."));
            if (!ACTIVE.equalsIgnoreCase(manager.getStatus())) {
                throw new IllegalArgumentException("Selected manager must be active.");
            }
        }

        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one internal employee must be selected.");
        }
        LinkedHashSet<Long> distinctEmployeeIds = new LinkedHashSet<>(employeeIds);
        if (distinctEmployeeIds.contains(null)) {
            throw new IllegalArgumentException("A valid internal employee must be selected.");
        }

        List<EmployeeEntity> employees = employeeRepository.findAllById(distinctEmployeeIds);
        if (employees.size() != distinctEmployeeIds.size()) {
            throw new IllegalArgumentException("One or more selected employees were not found.");
        }
        if (normalizedManagerId == null && !TYPE_OTHER.equals(normalizedType)) {
            Set<Long> eligibleManagerIds = getManagersForType(normalizedType).stream()
                    .map(EmployeeEntity::getEmployeeId)
                    .collect(Collectors.toSet());
            if (!eligibleManagerIds.containsAll(distinctEmployeeIds)) {
                throw new IllegalArgumentException(
                        "One or more selected employees do not match the selected manager type.");
            }
        }

        Long hodEmployeeId = TYPE_OTHER.equals(normalizedType)
                ? employeeRepository.findByUser_Id(hodUserId)
                        .map(EmployeeEntity::getEmployeeId)
                        .orElse(null)
                : null;
        for (EmployeeEntity employee : employees) {
            if (!ACTIVE.equalsIgnoreCase(employee.getStatus())) {
                throw new IllegalArgumentException("Inactive employees cannot be mapped.");
            }
            if (!INTERNAL.equalsIgnoreCase(employee.getRecruitmentType())) {
                throw new IllegalArgumentException("External employees cannot be mapped.");
            }
            if (hodEmployeeId != null && hodEmployeeId.equals(employee.getEmployeeId())) {
                throw new IllegalArgumentException(
                        "The selected reporting authority cannot be mapped as their own report.");
            }
        }

        return new MappingRequest(
                hodUserId,
                normalizedType,
                normalizedManagerId,
                normalizedProjectId,
                List.copyOf(distinctEmployeeIds));
    }

    private User requireReportingAuthority(Long userId) {
        User authority = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Selected reporting authority was not found."));
        if (resolveAuthorityType(authority) == null) {
            throw new IllegalArgumentException("Selected user must have the HOD or COO role.");
        }
        return authority;
    }

    private String resolveAuthorityType(User user) {
        if (hasRole(user, ROLE_COO)) {
            return TYPE_COO;
        }
        return hasRole(user, ROLE_HOD) ? TYPE_HOD : null;
    }

    private boolean hasRole(User user, String roleName) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().stream()
                        .anyMatch(role -> role != null && roleName.equalsIgnoreCase(role.getName()));
    }

    private String normalizeManagerType(String managerType) {
        if (managerType == null) {
            throw new IllegalArgumentException("A valid manager type is required.");
        }
        String normalized = managerType.trim().toUpperCase(Locale.ROOT);
        if (!TYPE_HOD.equals(normalized)
                && !TYPE_STM.equals(normalized)
                && !TYPE_PM.equals(normalized)
                && !TYPE_OTHER.equals(normalized)) {
            throw new IllegalArgumentException("A valid manager type is required.");
        }
        return normalized;
    }

    private EmployeeReportingMappingEntity toEntity(
            EmployeeReportingMappingEntity mapping,
            MappingRequest request,
            Long employeeId) {
        mapping.setHodUserId(request.hodUserId());
        mapping.setManagerType(request.managerType());
        mapping.setManagerEmployeeId(request.managerEmployeeId());
        mapping.setProjectId(request.projectId());
        mapping.setEmployeeId(employeeId);
        return mapping;
    }

    private record MappingRequest(
            Long hodUserId,
            String managerType,
            Long managerEmployeeId,
            Long projectId,
            List<Long> employeeIds) {
    }
}
