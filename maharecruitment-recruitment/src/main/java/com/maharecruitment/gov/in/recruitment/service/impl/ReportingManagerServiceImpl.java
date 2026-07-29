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
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.PositionMasterRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
public class ReportingManagerServiceImpl implements ReportingManagerService {

    private static final Logger log = LoggerFactory.getLogger(ReportingManagerServiceImpl.class);
    private static final String TYPE_STM = "STM";
    private static final String TYPE_PM = "PM";
    private static final String TYPE_OTHER = "OTHER";
    private static final String ROLE_STM = "ROLE_STM";
    private static final String ROLE_PM = "ROLE_PM";
    private static final String ACTIVE = "ACTIVE";
    private static final String ACTIVE_FLAG_Y = "Y";
    private static final String INTERNAL = "INTERNAL";
    private static final Set<String> STM_DESIGNATION_NAMES = Set.of(
            "STM",
            "SENIOR TECHNICAL MANAGER",
            "SENIOR TECHNICAL MANAGER (STM)");
    private static final Set<String> PM_DESIGNATION_NAMES = Set.of(
            "PM",
            "PROJECT MANAGER",
            "PROJECT MANAGER (PM)");
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
        List<Long> hodUserIds = new ArrayList<>();
        hodUserIds.addAll(userRepository.findDistinctUserIdsByRoleName("ROLE_HOD"));
        // Remove duplicates just in case
        hodUserIds = hodUserIds.stream().distinct().collect(Collectors.toList());

        List<User> hodUsers = userRepository.findAllById(hodUserIds);
        
        return hodUsers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName() + " (" + u.getEmail() + ")");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getManagersByType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Manager type is required.");
        }

        String normalizedType = normalizeManagerType(type);
        List<EmployeeEntity> managers;
        if (TYPE_STM.equals(normalizedType)) {
            managers = getActiveManagers(ROLE_STM, STM_DESIGNATION_NAMES, STM_MANAGER_NAME_PATTERN);
        } else if (TYPE_PM.equals(normalizedType)) {
            managers = getActiveManagers(ROLE_PM, PM_DESIGNATION_NAMES, PM_MANAGER_NAME_PATTERN);
        } else if (TYPE_OTHER.equals(normalizedType)) {
            managers = employeeRepository.findActiveEmployeesNotMappedAsStmOrPmManagers();
        } else {
            throw new IllegalArgumentException("Unsupported manager type: " + type);
        }
        
        return managers.stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getEmployeeId());
                    map.put("name", e.getFullName() + " (" + e.getEmployeeCode() + ")");
                    return map;
                }).collect(Collectors.toList());
    }

    private List<EmployeeEntity> getActiveManagers(
            String roleName,
            Set<String> managerNames,
            String managerNamePattern) {
        List<EmployeeEntity> roleOrDesignationManagers =
                employeeRepository.findActiveEmployeesByRoleNameOrDesignationNames(
                        roleName, managerNames, managerNamePattern);
        List<EmployeeEntity> positionManagers =
                positionRepository.findFilledActiveEmployeesByManagerNames(
                        managerNames,
                        managerNamePattern,
                        OrganizationRecordStatus.ACTIVE,
                        PositionStatus.FILLED,
                        ACTIVE);

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
                        "Please select Head of Department before selecting Other Employees.");
            }
            requireHod(hodUserId);
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
           User hod = userMap.get(m.getHodUserId());
           map.put("hodName", hod != null ? hod.getName() : "");

           map.put("managerEmployeeId", m.getManagerEmployeeId());
           if (TYPE_OTHER.equalsIgnoreCase(m.getManagerType())) {
               map.put("managerName", hod != null
                       ? "Directly Reports to HOD - " + hod.getName()
                       : "Directly Reports to HOD");
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
        log.info("Created {} reporting mapping(s) for HOD userId={} and managerType={}",
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
        if (TYPE_OTHER.equalsIgnoreCase(mapping.getManagerType())) {
            return mapping.getHodUserId();
        }
        if (mapping.getManagerEmployeeId() == null) {
            return null;
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
            throw new IllegalArgumentException("HOD selection is required.");
        }
        requireHod(hodUserId);

        String normalizedType = normalizeManagerType(managerType);
        Long normalizedManagerId = managerEmployeeId;
        Long normalizedProjectId = projectId;
        if (TYPE_STM.equals(normalizedType) || TYPE_PM.equals(normalizedType)) {
            if (managerEmployeeId == null) {
                throw new IllegalArgumentException("Manager selection is required.");
            }
            EmployeeEntity manager = employeeRepository.findById(managerEmployeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected manager was not found."));
            if (!ACTIVE.equalsIgnoreCase(manager.getStatus())) {
                throw new IllegalArgumentException("Selected manager must be active.");
            }
            if (normalizedProjectId != null) {
                requireActiveProject(normalizedProjectId);
            }
        } else {
            normalizedManagerId = null;
            normalizedProjectId = null;
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
                throw new IllegalArgumentException("The selected HOD cannot be mapped as their own report.");
            }
        }

        return new MappingRequest(
                hodUserId,
                normalizedType,
                normalizedManagerId,
                normalizedProjectId,
                List.copyOf(distinctEmployeeIds));
    }

    private User requireHod(Long hodUserId) {
        return userRepository.findById(hodUserId)
                .orElseThrow(() -> new IllegalArgumentException("Selected HOD was not found."));
    }

    private ProjectMst requireActiveProject(Long projectId) {
        ProjectMst project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Selected project was not found."));
        if (!ACTIVE_FLAG_Y.equalsIgnoreCase(project.getActiveFlag())) {
            throw new IllegalArgumentException("Selected project must be active.");
        }
        return project;
    }

    private String normalizeManagerType(String managerType) {
        if (managerType == null) {
            throw new IllegalArgumentException("A valid manager type is required.");
        }
        String normalized = managerType.trim().toUpperCase(Locale.ROOT);
        if (!TYPE_STM.equals(normalized) && !TYPE_PM.equals(normalized) && !TYPE_OTHER.equals(normalized)) {
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
