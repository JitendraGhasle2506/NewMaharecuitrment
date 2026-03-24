package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.repository.DepartmentAdvancePaymentRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;
import com.maharecruitment.gov.in.department.service.model.DepartmentRunningProjectView;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class DepartmentDashboardServiceImpl implements DepartmentDashboardService {

    private static final String DEFAULT_DEPARTMENT_TITLE = "Department";

    private final DepartmentProjectApplicationRepository projectApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentAdvancePaymentRepository advancePaymentRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final UserRepository userRepository;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public DepartmentDashboardServiceImpl(
            DepartmentProjectApplicationRepository projectApplicationRepository,
            EmployeeRepository employeeRepository,
            DepartmentAdvancePaymentRepository advancePaymentRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            UserRepository userRepository,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.projectApplicationRepository = projectApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.advancePaymentRepository = advancePaymentRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.userRepository = userRepository;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public DepartmentDashboardView getDashboard(Long departmentRegistrationId, Long userId) {
        DepartmentRegistrationEntity registration = resolveDepartmentRegistration(departmentRegistrationId, userId);
        DepartmentNameContext nameContext = resolveDepartmentNameContext(registration);
        if (registration == null || registration.getDepartmentRegistrationId() == null) {
            return emptyDashboard(nameContext);
        }
        Long resolvedRegistrationId = registration.getDepartmentRegistrationId();

        // Fetch all projects for the department
        List<DepartmentProjectApplicationEntity> allProjects = projectApplicationRepository
                .findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(resolvedRegistrationId);

        int registeredProjectCount = allProjects.size();

        // Determine running projects based on status
        List<DepartmentProjectApplicationEntity> runningProjectsEntities = allProjects.stream()
                .filter(p -> p.getApplicationStatus() == DepartmentApplicationStatus.AUDITOR_APPROVED
                        || p.getApplicationStatus() == DepartmentApplicationStatus.COMPLETED)
                .collect(Collectors.toList());

        int runningProjectCount = runningProjectsEntities.size();

        // Fetch all employees for the department
        List<EmployeeEntity> employees = employeeRepository
                .findByDepartmentRegistration_DepartmentRegistrationId(resolvedRegistrationId);

        int employeeCount = employees.size();

        // Calculate pending payments: projects approved by auditor but not yet paid (including advance payment)
        List<Long> initiatedAppIds = advancePaymentRepository.findApplicationIdsByDepartmentRegistrationId(resolvedRegistrationId);

        // Map running projects to view objects
        List<DepartmentRunningProjectView> runningProjects = runningProjectsEntities.stream()
                .map(p -> new DepartmentRunningProjectView(
                        p.getProjectCode() != null ? p.getProjectCode() : "N/A",
                        p.getProjectName(),
                        p.getCreatedDate() != null ? p.getCreatedDate().toLocalDate() : LocalDate.now(),
                        (int) employees.stream().filter(e -> Objects.equals(e.getRequestId(), p.getRequestId())).count(),
                        p.getApplicationStatus().getDisplayName(),
                        !initiatedAppIds.contains(p.getDepartmentProjectApplicationId()),
                        p.getDepartmentProjectApplicationId()))
                .collect(Collectors.toList());

        int pendingPaymentCount = (int) runningProjectsEntities.stream()
                .filter(p -> !initiatedAppIds.contains(p.getDepartmentProjectApplicationId()))
                .count();

        return new DepartmentDashboardView(
                nameContext.departmentTitle(),
                nameContext.subDepartmentName(),
                registeredProjectCount,
                employeeCount,
                runningProjectCount,
                pendingPaymentCount,
                runningProjects,
                LocalDate.now());
    }

    private DepartmentDashboardView emptyDashboard(DepartmentNameContext nameContext) {
        return new DepartmentDashboardView(
                nameContext.departmentTitle(),
                nameContext.subDepartmentName(),
                0,
                0,
                0,
                0,
                List.of(),
                LocalDate.now());
    }

    private DepartmentRegistrationEntity resolveDepartmentRegistration(Long departmentRegistrationId, Long userId) {
        if (departmentRegistrationId != null) {
            DepartmentRegistrationEntity registration = departmentRegistrationRepository.findById(departmentRegistrationId)
                    .orElse(null);
            if (registration != null) {
                return registration;
            }
        }

        if (userId != null) {
            DepartmentRegistrationEntity fromUser = userRepository.findById(userId)
                    .map(user -> user.getDepartmentRegistrationId())
                    .orElse(null);
            if (fromUser != null) {
                return fromUser;
            }
        }

        if (departmentRegistrationId == null) {
            return null;
        }

        List<DepartmentRegistrationEntity> registrations = departmentRegistrationRepository
                .findByDepartmentIdOrderByCreatedAtAsc(departmentRegistrationId);
        if (registrations.isEmpty()) {
            return null;
        }

        return registrations.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getActive()))
                .findFirst()
                .orElse(registrations.get(0));
    }

    private DepartmentNameContext resolveDepartmentNameContext(DepartmentRegistrationEntity registration) {
        if (registration == null) {
            return new DepartmentNameContext(resolveDashboardTitle(null), null);
        }

        String departmentName = resolveDepartmentName(registration);
        String subDepartmentName = resolveSubDepartmentName(registration);
        return new DepartmentNameContext(
                resolveDashboardTitle(departmentName),
                subDepartmentName);
    }

    private String resolveDepartmentName(DepartmentRegistrationEntity registration) {
        Long departmentId = registration.getDepartmentId();
        if (departmentId != null) {
            String masterDepartmentName = departmentMstRepository.findById(departmentId)
                    .map(DepartmentMst::getDepartmentName)
                    .orElse(null);
            if (StringUtils.hasText(masterDepartmentName)) {
                return masterDepartmentName.trim();
            }
        }

        if (StringUtils.hasText(registration.getDepartmentName())) {
            return registration.getDepartmentName().trim();
        }
        return null;
    }

    private String resolveSubDepartmentName(DepartmentRegistrationEntity registration) {
        Long subDepartmentId = registration.getSubDeptId();
        if (subDepartmentId == null) {
            return null;
        }

        String masterSubDepartmentName = subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .orElse(null);
        if (StringUtils.hasText(masterSubDepartmentName)) {
            return masterSubDepartmentName.trim();
        }
        return null;
    }

    private String resolveDashboardTitle(String resolvedDepartmentName) {
        if (StringUtils.hasText(resolvedDepartmentName)) {
            return resolvedDepartmentName.trim();
        }
        return DEFAULT_DEPARTMENT_TITLE;
    }

    private record DepartmentNameContext(String departmentTitle, String subDepartmentName) {
    }
}
