package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.web.service.dashboard.RoleLandingService;
import com.maharecruitment.gov.in.web.service.dashboard.model.RoleDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.RoleTaskView;

@Service
public class RoleLandingServiceImpl implements RoleLandingService {

    private static final Map<String, String> TITLE_BY_PATH = Map.ofEntries(
            Map.entry("/admin/dashboard", "Admin Dashboard"),
            Map.entry("/user/dashboard", "User Dashboard"),
            Map.entry("/agency/dashboard", "Agency Dashboard"),
            Map.entry("/stm/dashboard", "STM Dashboard"),
            Map.entry("/pm/dashboard", "PM Dashboard"),
            Map.entry("/hod1/dashboard", "HOD-1 Dashboard"),
            Map.entry("/hod2/dashboard", "HOD-2 Dashboard"),
            Map.entry("/coo/dashboard", "COO Dashboard"),
            Map.entry("/employee/dashboard", "Employee Dashboard"),
            Map.entry("/department/home", "Department Home"),
            Map.entry("/pension", "Pension Module"),
            Map.entry("/hrms", "HRMS Module"),
            Map.entry("/payroll", "Payroll Module")
    );

    @Override
    public RoleDashboardView getDashboardByPath(String path) {
        String title = TITLE_BY_PATH.getOrDefault(path, "Role Dashboard");
        String subtitle = "Operational landing page for " + title;

        return new RoleDashboardView(
                title,
                subtitle,
                24,
                31,
                118,
                42,
                9,
                5,
                List.of(
                        new RoleTaskView("Review onboarding requests", "HR Desk", "Open"),
                        new RoleTaskView("Verify project staffing", "PMO", "In Progress"),
                        new RoleTaskView("Close pending approvals", "Section Head", "Pending"),
                        new RoleTaskView("Publish weekly report", "Operations", "Open")
                )
        );
    }
}
