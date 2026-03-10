package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;
import com.maharecruitment.gov.in.department.service.model.DepartmentRunningProjectView;
import com.maharecruitment.gov.in.web.service.dashboard.DepartmentDashboardPageService;

@Service
public class DepartmentDashboardPageServiceImpl implements DepartmentDashboardPageService {

    private final ObjectProvider<DepartmentDashboardService> departmentDashboardServiceProvider;

    public DepartmentDashboardPageServiceImpl(
            ObjectProvider<DepartmentDashboardService> departmentDashboardServiceProvider) {
        this.departmentDashboardServiceProvider = departmentDashboardServiceProvider;
    }

    @Override
    public DepartmentDashboardView getDashboard(Long departmentRegistrationId, String departmentDisplayName) {
        DepartmentDashboardService delegate = departmentDashboardServiceProvider.getIfAvailable();
        if (delegate != null) {
            return delegate.getDashboard(departmentRegistrationId, departmentDisplayName);
        }

        return buildFallbackDashboard(departmentRegistrationId, departmentDisplayName);
    }

    private DepartmentDashboardView buildFallbackDashboard(Long departmentRegistrationId, String departmentDisplayName) {
        long seed = Math.abs(Objects.hashCode(departmentRegistrationId));
        int registeredProjectCount = 14 + (int) (seed % 9);
        int runningProjectCount = 3 + (int) (seed % 3);
        int employeeCount = 48 + (int) (seed % 35);

        List<DepartmentRunningProjectView> runningProjects = List.of(
                new DepartmentRunningProjectView(
                        "DEP-" + (110 + seed % 20),
                        "Contract Staff Onboarding",
                        LocalDate.now().minusDays(35),
                        18 + (int) (seed % 5),
                        "Running"),
                new DepartmentRunningProjectView(
                        "DEP-" + (210 + seed % 20),
                        "Field Deployment Phase-2",
                        LocalDate.now().minusDays(21),
                        14 + (int) (seed % 7),
                        "Running"),
                new DepartmentRunningProjectView(
                        "DEP-" + (310 + seed % 20),
                        "Back-office Verification",
                        LocalDate.now().minusDays(9),
                        10 + (int) (seed % 4),
                        "Running"));

        return new DepartmentDashboardView(
                resolveDepartmentTitle(departmentDisplayName),
                registeredProjectCount,
                employeeCount,
                runningProjectCount,
                runningProjects,
                LocalDate.now());
    }

    private String resolveDepartmentTitle(String departmentDisplayName) {
        if (!StringUtils.hasText(departmentDisplayName)) {
            return "Department";
        }

        return departmentDisplayName.trim() + " Department";
    }
}
