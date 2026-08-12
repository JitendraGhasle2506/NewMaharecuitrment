package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.DesignationRoleAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationEmployeeRoleView;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentResult;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationRoleAssignmentView;

@Service
public class DesignationRoleAssignmentServiceImpl implements DesignationRoleAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DesignationRoleAssignmentServiceImpl.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String ACTIVE_FLAG_Y = "Y";
    private static final Set<String> ASSIGNABLE_ROLE_NAMES = Set.of(
            "ROLE_COO",
            "ROLE_EMPLOYEE",
            "ROLE_HOD",
            "ROLE_HR",
            "ROLE_INFRA",
            "ROLE_MD",
            "ROLE_PM",
            "ROLE_STM",
            "ROLE_USER");
    private static final Map<String, String> ROLE_ALIASES = Map.of(
            "CHIEF_OPERATING_OFFICER", "ROLE_COO",
            "HEAD_OF_DEPARTMENT", "ROLE_HOD",
            "SENIOR_TECHNICAL_MANAGER", "ROLE_STM",
            "PROJECT_MANAGER", "ROLE_PM",
            "HUMAN_RESOURCES", "ROLE_HR",
            "MANAGING_DIRECTOR", "ROLE_MD",
            "INFRASTRUCTURE", "ROLE_INFRA");

    private final EmployeeRepository employeeRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DesignationRoleAssignmentServiceImpl(
            EmployeeRepository employeeRepository,
            ManpowerDesignationMasterRepository designationRepository,
            RoleRepository roleRepository,
            UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.designationRepository = designationRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAssignableRoleNames() {
        return loadAssignableRolesByName().keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationRoleAssignmentView> getAssignments(String searchTerm) {
        List<ManpowerDesignationMaster> designations = designationRepository
                .findByActiveFlagIgnoreCaseOrderByDesignationNameAsc(ACTIVE_FLAG_Y);
        Map<Long, List<EmployeeEntity>> employeesByDesignationId = employeeRepository
                .findActiveEmployeesForDesignationRoleAssignment(ACTIVE, ACTIVE_FLAG_Y)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        employee -> employee.getDesignation().getDesignationId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        Map<String, Role> rolesByName = loadAssignableRolesByName();
        String normalizedSearch = normalizeSearch(searchTerm);

        return designations.stream()
                .map(designation -> toView(
                        designation,
                        employeesByDesignationId.getOrDefault(designation.getDesignationId(), List.of()),
                        rolesByName))
                .filter(view -> matchesSearch(view, normalizedSearch))
                .toList();
    }

    @Override
    @Transactional
    public DesignationRoleAssignmentResult configureAndAssign(Long designationId, String roleName) {
        if (designationId == null) {
            throw new IllegalArgumentException("Designation is required.");
        }
        ManpowerDesignationMaster designation = designationRepository
                .findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE_FLAG_Y)
                .orElseThrow(() -> new IllegalArgumentException("Active designation was not found."));
        Role role = requireAssignableRole(roleName);

        designation.setRoleName(role.getName());
        designationRepository.save(designation);
        List<EmployeeEntity> employees = employeeRepository
                .findByDesignation_DesignationIdAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                        designationId,
                        ACTIVE);
        MutableAssignmentCounts counts = new MutableAssignmentCounts();
        Map<Long, User> changedUsersById = new LinkedHashMap<>();
        assignRole(employees, role, counts, changedUsersById);
        userRepository.saveAll(new ArrayList<>(changedUsersById.values()));

        log.info("Configured designationId={} with role={} and assigned role to {} user(s)",
                designationId, role.getName(), counts.assignedUsers);
        return counts.toResult(0);
    }

    @Override
    @Transactional
    public DesignationRoleAssignmentResult assignAllConfiguredRoles() {
        Map<String, Role> rolesByName = loadAssignableRolesByName();
        Map<Long, ManpowerDesignationMaster> designationsById = designationRepository
                .findByActiveFlagIgnoreCaseOrderByDesignationNameAsc(ACTIVE_FLAG_Y)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ManpowerDesignationMaster::getDesignationId,
                        designation -> designation));
        Map<Long, List<EmployeeEntity>> employeesByDesignationId = employeeRepository
                .findActiveEmployeesForDesignationRoleAssignment(ACTIVE, ACTIVE_FLAG_Y)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        employee -> employee.getDesignation().getDesignationId()));

        MutableAssignmentCounts counts = new MutableAssignmentCounts();
        Map<Long, User> changedUsersById = new LinkedHashMap<>();
        int skippedDesignations = 0;
        for (ManpowerDesignationMaster designation : designationsById.values()) {
            Role role = rolesByName.get(normalizeConfiguredRoleName(designation.getRoleName()));
            if (role == null) {
                skippedDesignations++;
                continue;
            }
            assignRole(
                    employeesByDesignationId.getOrDefault(designation.getDesignationId(), List.of()),
                    role,
                    counts,
                    changedUsersById);
        }
        userRepository.saveAll(new ArrayList<>(changedUsersById.values()));

        log.info("Applied configured designation roles: assignedUsers={}, skippedDesignations={}",
                counts.assignedUsers, skippedDesignations);
        return counts.toResult(skippedDesignations);
    }

    private DesignationRoleAssignmentView toView(
            ManpowerDesignationMaster designation,
            List<EmployeeEntity> employees,
            Map<String, Role> rolesByName) {
        String configuredRoleName = normalizeConfiguredRoleName(designation.getRoleName());
        Role configuredRole = rolesByName.get(configuredRoleName);
        List<DesignationEmployeeRoleView> employeeViews = employees.stream()
                .map(employee -> toEmployeeView(employee, configuredRoleName, configuredRole != null))
                .sorted(Comparator
                        .comparing(DesignationEmployeeRoleView::employeeName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DesignationEmployeeRoleView::employeeId))
                .toList();
        long linkedUsers = employeeViews.stream().filter(view -> view.userId() != null).count();
        long assignedUsers = employeeViews.stream()
                .filter(DesignationEmployeeRoleView::configuredRoleAssigned)
                .count();
        long pendingUsers = employeeViews.stream()
                .filter(view -> view.userId() != null && view.userActive() && !view.configuredRoleAssigned())
                .count();

        return new DesignationRoleAssignmentView(
                designation.getDesignationId(),
                safeText(designation.getDesignationName()),
                safeText(designation.getCategory()),
                configuredRoleName,
                configuredRole != null,
                employeeViews.size(),
                linkedUsers,
                assignedUsers,
                pendingUsers,
                employeeViews);
    }

    private DesignationEmployeeRoleView toEmployeeView(
            EmployeeEntity employee,
            String configuredRoleName,
            boolean configuredRoleAvailable) {
        User user = employee.getUser();
        List<String> currentRoleNames = user == null || user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .filter(role -> role != null && role.getName() != null)
                        .map(Role::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
        boolean assigned = configuredRoleAvailable && currentRoleNames.stream()
                .anyMatch(configuredRoleName::equalsIgnoreCase);
        return new DesignationEmployeeRoleView(
                employee.getEmployeeId(),
                safeText(employee.getEmployeeCode()),
                safeText(employee.getFullName()),
                user == null ? null : user.getId(),
                user != null && Boolean.TRUE.equals(user.getActive()),
                currentRoleNames,
                assigned);
    }

    private void assignRole(
            List<EmployeeEntity> employees,
            Role role,
            MutableAssignmentCounts counts,
            Map<Long, User> changedUsersById) {
        for (EmployeeEntity employee : employees) {
            User user = employee.getUser();
            if (user == null || user.getId() == null) {
                counts.missingUserAccounts++;
                continue;
            }
            if (!Boolean.TRUE.equals(user.getActive())) {
                counts.inactiveUserAccounts++;
                continue;
            }
            boolean alreadyAssigned = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(currentRole -> currentRole != null
                            && role.getName().equalsIgnoreCase(currentRole.getName()));
            if (alreadyAssigned) {
                counts.alreadyAssignedUsers++;
                continue;
            }
            user.addRole(role);
            changedUsersById.put(user.getId(), user);
            counts.assignedUsers++;
        }
    }

    private Role requireAssignableRole(String roleName) {
        String normalizedRoleName = normalizeConfiguredRoleName(roleName);
        if (!ASSIGNABLE_ROLE_NAMES.contains(normalizedRoleName)) {
            throw new IllegalArgumentException("Select a valid workforce role.");
        }
        return roleRepository.findByNameIgnoreCase(normalizedRoleName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected role is not configured in Role Management."));
    }

    private Map<String, Role> loadAssignableRolesByName() {
        Map<String, Role> rolesByName = new HashMap<>();
        roleRepository.findAllByOrderByNameAsc().stream()
                .filter(role -> role.getName() != null)
                .filter(role -> ASSIGNABLE_ROLE_NAMES.contains(role.getName().toUpperCase(Locale.ROOT)))
                .forEach(role -> rolesByName.put(role.getName().toUpperCase(Locale.ROOT), role));
        return rolesByName;
    }

    private boolean matchesSearch(DesignationRoleAssignmentView view, String searchTerm) {
        if (searchTerm == null) {
            return true;
        }
        return contains(view.designationName(), searchTerm)
                || contains(view.category(), searchTerm)
                || contains(view.configuredRoleName(), searchTerm)
                || view.employees().stream().anyMatch(employee ->
                        contains(employee.employeeName(), searchTerm)
                                || contains(employee.employeeCode(), searchTerm));
    }

    private boolean contains(String value, String searchTerm) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchTerm);
    }

    private String normalizeSearch(String searchTerm) {
        return searchTerm == null || searchTerm.isBlank()
                ? null
                : searchTerm.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeConfiguredRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        String normalized = roleName.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return ROLE_ALIASES.getOrDefault(normalized, "ROLE_" + normalized);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class MutableAssignmentCounts {
        private int assignedUsers;
        private int alreadyAssignedUsers;
        private int missingUserAccounts;
        private int inactiveUserAccounts;

        private DesignationRoleAssignmentResult toResult(int skippedDesignations) {
            return new DesignationRoleAssignmentResult(
                    assignedUsers,
                    alreadyAssignedUsers,
                    missingUserAccounts,
                    inactiveUserAccounts,
                    skippedDesignations);
        }
    }
}
