package com.maharecruitment.gov.in.attendance.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

/**
 * Leave and tour requests of INTERNAL employees may only be decided by senior roles; other approvers
 * in the reporting chain can still see them. MAHAIT and other recruitment types are not affected.
 */
@Service
@Transactional(readOnly = true)
public class InternalEmployeeApprovalPolicy {

    public static final Set<String> INTERNAL_APPROVER_ROLES = Set.of(
            "ROLE_STM", "ROLE_COO", "ROLE_HOD", "ROLE_MD", "ROLE_CTO", "ROLE_CFO");

    private static final String INTERNAL = "INTERNAL";

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public InternalEmployeeApprovalPolicy(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    public boolean hasInternalApproverRole(Long actorUserId) {
        if (actorUserId == null) {
            return false;
        }
        return userRepository.findById(actorUserId)
                .map(user -> user.getRoles().stream()
                        .map(Role::getName)
                        .anyMatch(name -> name != null
                                && INTERNAL_APPROVER_ROLES.contains(name.trim().toUpperCase(Locale.ROOT))))
                .orElse(false);
    }

    public boolean isInternal(EmployeeEntity employee) {
        return employee != null
                && employee.getRecruitmentType() != null
                && INTERNAL.equalsIgnoreCase(employee.getRecruitmentType().trim());
    }

    public void requireCanDecide(Long actorUserId, Long employeeId, String requestLabel) {
        boolean internal = employeeId != null
                && employeeRepository.findById(employeeId).map(this::isInternal).orElse(false);
        if (internal && !hasInternalApproverRole(actorUserId)) {
            throw new IllegalArgumentException("You can view this " + requestLabel
                    + " application, but only HOD, STM, COO, CTO, CFO or MD can approve or reject"
                    + " requests of internal employees.");
        }
    }
}
