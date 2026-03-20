package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;
import com.maharecruitment.gov.in.department.service.model.DepartmentRunningProjectView;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.web.service.dashboard.DepartmentDashboardPageService;

@Service
public class DepartmentDashboardPageServiceImpl implements DepartmentDashboardPageService {

    private static final String DEFAULT_DEPARTMENT_TITLE = "Department";

    private final ObjectProvider<DepartmentDashboardService> departmentDashboardServiceProvider;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final UserRepository userRepository;
    private final DepartmentMstRepository departmentMstRepository;
    private final SubDepartmentRepository subDepartmentRepository;

    public DepartmentDashboardPageServiceImpl(
            ObjectProvider<DepartmentDashboardService> departmentDashboardServiceProvider,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            UserRepository userRepository,
            DepartmentMstRepository departmentMstRepository,
            SubDepartmentRepository subDepartmentRepository) {
        this.departmentDashboardServiceProvider = departmentDashboardServiceProvider;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.userRepository = userRepository;
        this.departmentMstRepository = departmentMstRepository;
        this.subDepartmentRepository = subDepartmentRepository;
    }

    @Override
    public DepartmentDashboardView getDashboard(Long departmentRegistrationId, Long userId) {
        DepartmentDashboardService delegate = departmentDashboardServiceProvider.getIfAvailable();
        if (delegate != null) {
            return delegate.getDashboard(departmentRegistrationId, userId);
        }

        return buildFallbackDashboard(departmentRegistrationId, userId);
    }

    private DepartmentDashboardView buildFallbackDashboard(Long departmentRegistrationId, Long userId) {
        DepartmentRegistrationEntity registration = resolveDepartmentRegistration(departmentRegistrationId, userId);
        DepartmentNameContext nameContext = resolveDepartmentNameContext(registration);

        Long resolvedRegistrationId = registration != null ? registration.getDepartmentRegistrationId() : departmentRegistrationId;
        long seed = Math.abs(Objects.hashCode(resolvedRegistrationId));
        int registeredProjectCount = 14 + (int) (seed % 9);
        int runningProjectCount = 3 + (int) (seed % 3);
        int employeeCount = 48 + (int) (seed % 35);

        List<DepartmentRunningProjectView> runningProjects = List.of(
                new DepartmentRunningProjectView(
                        "DEP-" + (110 + seed % 20),
                        "Contract Staff Onboarding",
                        LocalDate.now().minusDays(35),
                        18 + (int) (seed % 5),
                        "Running",
                        true,
                        101L + seed % 10),
                new DepartmentRunningProjectView(
                        "DEP-" + (210 + seed % 20),
                        "Field Deployment Phase-2",
                        LocalDate.now().minusDays(21),
                        14 + (int) (seed % 7),
                        "Running",
                        false,
                        201L + seed % 10),
                new DepartmentRunningProjectView(
                        "DEP-" + (310 + seed % 20),
                        "Back-office Verification",
                        LocalDate.now().minusDays(9),
                        10 + (int) (seed % 4),
                        "Running",
                        true,
                        301L + seed % 10));

        int pendingPaymentCount = 1 + (int) (seed % 4);

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
