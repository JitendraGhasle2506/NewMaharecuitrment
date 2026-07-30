package com.maharecruitment.gov.in.department.service;

import com.maharecruitment.gov.in.department.service.model.DepartmentDashboardView;

public interface DepartmentDashboardService {

    DepartmentDashboardView getDashboard(Long departmentRegistrationId, Long userId);
}
