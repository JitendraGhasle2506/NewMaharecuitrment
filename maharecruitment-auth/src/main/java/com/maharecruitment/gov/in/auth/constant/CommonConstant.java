package com.maharecruitment.gov.in.auth.constant;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommonConstant {

    private static final Map<String, String> DASHBOARD_URLS = createDashboardUrls();

    private CommonConstant() {
    }

    public static Map<String, String> getDashboardUrls() {
        return DASHBOARD_URLS;
    }

    private static Map<String, String> createDashboardUrls() {
        Map<String, String> roleTargetUrlMap = new LinkedHashMap<>();
        roleTargetUrlMap.put("ROLE_USER", "/user/dashboard");
        roleTargetUrlMap.put("ROLE_ADMIN", "/admin/dashboard");
        roleTargetUrlMap.put("ROLE_HR", "/hr/dashboard");
        roleTargetUrlMap.put("ROLE_AGENCY", "/agency/dashboard");
        roleTargetUrlMap.put("ROLE_STM", "/stm/dashboard");
        roleTargetUrlMap.put("ROLE_PM", "/pm/dashboard");
        roleTargetUrlMap.put("ROLE_HOD1", "/hod1/dashboard");
        roleTargetUrlMap.put("ROLE_HOD2", "/hod2/dashboard");
        roleTargetUrlMap.put("ROLE_COO", "/coo/dashboard");
        roleTargetUrlMap.put("ROLE_AUDITOR", "/auditor/department-requests");
        roleTargetUrlMap.put("ROLE_DEPARTMENT", "/department/home");
        roleTargetUrlMap.put("ROLE_EMPLOYEE", "/employee/dashboard");

        // Existing project module roles
        roleTargetUrlMap.put("ROLE_COMMON_MANAGER", "/common");
        roleTargetUrlMap.put("ROLE_ATTENDANCE_MANAGER", "/attendance");
        roleTargetUrlMap.put("ROLE_ESERVICEBOOK_MANAGER", "/eservicebook");
        roleTargetUrlMap.put("ROLE_PENSION_MANAGER", "/pension");
        roleTargetUrlMap.put("ROLE_HRMS_MANAGER", "/hrms");
        roleTargetUrlMap.put("ROLE_PAYROLL_MANAGER", "/payroll");

        return Map.copyOf(roleTargetUrlMap);
    }
}
