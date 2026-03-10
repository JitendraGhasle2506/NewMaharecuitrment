package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.service.DepartmentDashboardService;
import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;
import com.maharecruitment.gov.in.department.service.model.DepartmentRunningProjectView;

@Service
public class DepartmentDashboardServiceImpl implements DepartmentDashboardService {

    @Override
    public DepartmentDashboardView getDashboard(Long departmentRegistrationId, String departmentDisplayName) {
        long seed = Math.abs(Objects.hashCode(departmentRegistrationId));
        int registeredProjectCount = 14 + (int) (seed % 9);
        int runningProjectCount = 3 + (int) (seed % 3);
        int employeeCount = 48 + (int) (seed % 35);

        String title = StringUtils.hasText(departmentDisplayName)
                ? departmentDisplayName.trim() + " Department"
                : "Department";

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
                title,
                registeredProjectCount,
                employeeCount,
                runningProjectCount,
                runningProjects,
                LocalDate.now());
    }
}
