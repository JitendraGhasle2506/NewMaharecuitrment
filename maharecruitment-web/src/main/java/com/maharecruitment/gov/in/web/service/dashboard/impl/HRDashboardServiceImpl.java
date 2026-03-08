package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.web.service.dashboard.HRDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.DepartmentOnboardingView;
import com.maharecruitment.gov.in.web.service.dashboard.model.HRDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.ProjectWorkforceView;

@Service
public class HRDashboardServiceImpl implements HRDashboardService {

    @Override
    public HRDashboardView getDashboard() {
        int internalEmployees = 1240;
        int externalEmployees = 486;
        int totalEmployees = internalEmployees + externalEmployees;

        return new HRDashboardView(
                38,
                72,
                internalEmployees,
                externalEmployees,
                totalEmployees,
                19,
                27,
                "2.8%",
                (internalEmployees * 100) / totalEmployees,
                (externalEmployees * 100) / totalEmployees,
                List.of(
                        new DepartmentOnboardingView("Finance", 14, 20),
                        new DepartmentOnboardingView("Health", 22, 30),
                        new DepartmentOnboardingView("Education", 18, 25),
                        new DepartmentOnboardingView("Transport", 10, 18),
                        new DepartmentOnboardingView("Rural Development", 8, 12)
                ),
                List.of(
                        new ProjectWorkforceView("PRJ-1012", "Digital Attendance Rollout", 46, 18, "In Progress"),
                        new ProjectWorkforceView("PRJ-0984", "Recruitment Workflow Revamp", 32, 12, "Planning"),
                        new ProjectWorkforceView("PRJ-0941", "eServicebook Migration", 28, 6, "In Progress"),
                        new ProjectWorkforceView("PRJ-0899", "Payroll Reconciliation", 20, 9, "UAT"),
                        new ProjectWorkforceView("PRJ-0871", "Agency Contract Tracking", 15, 22, "On Hold")
                )
        );
    }
}
