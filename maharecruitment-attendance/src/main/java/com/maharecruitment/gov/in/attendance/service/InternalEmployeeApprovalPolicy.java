package com.maharecruitment.gov.in.attendance.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.repository.InternalApproverRoleRepository;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

/**
 * Leave and tour requests of INTERNAL employees may only be decided by the roles chosen on
 * Admin > Internal Approver Roles; other approvers in the reporting chain can still see them.
 * The roles are read on every check, so a change applies to the next approval action.
 * MAHAIT and other recruitment types are not affected.
 */
@Service
@Transactional(readOnly = true)
public class InternalEmployeeApprovalPolicy {

    private static final String INTERNAL = "INTERNAL";
    private static final String ROLE_PREFIX = "ROLE_";

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final InternalApproverRoleRepository approverRoleRepository;

    public InternalEmployeeApprovalPolicy(EmployeeRepository employeeRepository, UserRepository userRepository,
            InternalApproverRoleRepository approverRoleRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.approverRoleRepository = approverRoleRepository;
    }

    /** Configured approver role names, upper-case. */
    public Set<String> approverRoleNames() {
        return approverRoleRepository.findApproverRoleNames().stream()
                .map(InternalEmployeeApprovalPolicy::normalize)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasInternalApproverRole(Long actorUserId) {
        if (actorUserId == null) {
            return false;
        }
        Set<String> approverRoles = approverRoleNames();
        if (approverRoles.isEmpty()) {
            return false;
        }
        return userRepository.findById(actorUserId)
                .map(user -> user.getRoles().stream()
                        .map(Role::getName)
                        .anyMatch(name -> name != null && approverRoles.contains(normalize(name))))
                .orElse(false);
    }

    public boolean isInternal(EmployeeEntity employee) {
        return employee != null
                && employee.getRecruitmentType() != null
                && INTERNAL.equalsIgnoreCase(employee.getRecruitmentType().trim());
    }

    /** Why an approver without a configured role can only view an internal employee's request. */
    public String viewOnlyReason() {
        String label = approverRoleLabel(approverRoleRepository.findApproverRoleNames());
        return label.isEmpty()
                ? "No role is set to approve internal employee requests. Ask the administrator to set one."
                : "Only " + label + " can approve internal employee requests.";
    }

    public void requireCanDecide(Long actorUserId, Long employeeId, String requestLabel) {
        boolean internal = employeeId != null
                && employeeRepository.findById(employeeId).map(this::isInternal).orElse(false);
        if (internal && !hasInternalApproverRole(actorUserId)) {
            throw new IllegalArgumentException("You can view this " + requestLabel + " application, but you cannot"
                    + " approve or reject it. " + viewOnlyReason());
        }
    }

    /** "HOD, PM or STM" from role names; empty when none are given. */
    public static String approverRoleLabel(Collection<String> roleNames) {
        List<String> names = roleNames.stream()
                .map(InternalEmployeeApprovalPolicy::shortName)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
        if (names.size() <= 1) {
            return names.isEmpty() ? "" : names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.getLast();
    }

    /** ROLE_HOD becomes HOD. */
    public static String shortName(String roleName) {
        String name = normalize(roleName);
        return name.startsWith(ROLE_PREFIX) ? name.substring(ROLE_PREFIX.length()) : name;
    }

    private static String normalize(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
    }
}
