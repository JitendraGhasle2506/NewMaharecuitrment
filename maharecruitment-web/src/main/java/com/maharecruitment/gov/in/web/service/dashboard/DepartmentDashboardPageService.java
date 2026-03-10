package com.maharecruitment.gov.in.web.service.dashboard;

import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;

public interface DepartmentDashboardPageService {

    DepartmentDashboardView getDashboard(Long departmentRegistrationId, String departmentDisplayName);
}
