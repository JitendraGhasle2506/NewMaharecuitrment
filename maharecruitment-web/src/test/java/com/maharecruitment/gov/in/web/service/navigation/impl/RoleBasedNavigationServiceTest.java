package com.maharecruitment.gov.in.web.service.navigation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoleBasedNavigationServiceTest {

    private final RoleBasedNavigationService service = new RoleBasedNavigationService();

    @Test
    void canAccessUrlAllowsAdminOnlyRoutesForAdmin() {
        assertTrue(service.canAccessUrl("/attendance", List.of("ROLE_ADMIN")));
        assertTrue(service.canAccessUrl("/attendance/manual", List.of("ROLE_ADMIN")));
    }

    @Test
    void canAccessUrlRejectsHrRouteForAdminOnlyUser() {
        assertFalse(service.canAccessUrl("/hr/dashboard", List.of("ROLE_ADMIN")));
    }

    @Test
    void canAccessUrlAllowsSharedHrAuditorSubmenuForAuditor() {
        assertTrue(service.canAccessUrl("/hr/department/payment/list", List.of("ROLE_AUDITOR")));
    }

    @Test
    void executiveRolesResolveToSharedCooDashboard() {
        assertEquals("/coo/dashboard", service.resolveHomeUrl(List.of("ROLE_CTO")));
        assertEquals("/coo/dashboard", service.resolveHomeUrl(List.of("ROLE_CFO")));
        assertEquals("/coo/dashboard", service.resolveHomeUrl(List.of("ROLE_COO")));
        assertTrue(service.canAccessUrl("/coo/dashboard", List.of("ROLE_CTO")));
        assertTrue(service.canAccessUrl("/coo/dashboard", List.of("ROLE_CFO")));
        assertTrue(service.canAccessUrl("/coo/dashboard", List.of("ROLE_COO")));
        assertFalse(service.canAccessUrl("/coo/dashboard", List.of("ROLE_AUDITOR")));
    }

    @Test
    void stmUsesAndCanAccessTheSharedTeamDashboard() {
        assertEquals("/hod1/dashboard", service.resolveHomeUrl(List.of("ROLE_STM")));
        assertTrue(service.canAccessUrl("/hod1/dashboard", List.of("ROLE_STM")));
        assertTrue(service.canAccessUrl("/stm/dashboard", List.of("ROLE_STM")));
        assertFalse(service.canAccessUrl("/hod2/dashboard", List.of("ROLE_STM")));
    }

    @Test
    void canAccessUrlAllowsAuthenticatedFallbackForGeneralPages() {
        assertTrue(service.canAccessUrl("/master/designations", List.of("ROLE_HR")));
        assertTrue(service.canAccessUrl("/common", List.of("ROLE_DEPARTMENT")));
    }

    @Test
    void canAccessTeamAttendanceForAnyAuthenticatedReportingAuthority() {
        assertTrue(service.canAccessUrl(
                "/reporting-manager/attendance/view?empId=101",
                List.of("ROLE_EMPLOYEE")));
        assertFalse(service.canAccessUrl("/reporting-manager/attendance", List.of()));
    }

    @Test
    void attendanceApprovalRoutesAllowEveryAuthenticatedRole() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_HR", "ROLE_EMPLOYEE", "ROLE_COO",
                "ROLE_CTO", "ROLE_CFO", "ROLE_DEPARTMENT", "ROLE_AUDITOR")) {
            assertTrue(service.canAccessUrl("/hod1/manual-attendance", List.of(role)));
            assertTrue(service.canAccessUrl("/hod1/manual-attendance/details?userId=10", List.of(role)));
            assertTrue(service.canAccessUrl("/hod1/leaveApprovals", List.of(role)));
        }
        assertFalse(service.canAccessUrl("/hod1/manual-attendance", List.of()));
        assertFalse(service.canAccessUrl("/hod1/leaveApprovals", List.of()));
    }

    @Test
    void canAccessUrlRejectsBlankSubmenuUrl() {
        assertFalse(service.canAccessUrl(null, List.of("ROLE_ADMIN")));
        assertFalse(service.canAccessUrl("   ", List.of("ROLE_ADMIN")));
        assertFalse(service.canAccessUrl("#", List.of("ROLE_ADMIN")));
    }

    @Test
    void canAccessUrlRejectsExternalUrls() {
        assertFalse(service.canAccessUrl("https://evil.example.com/login", List.of("ROLE_ADMIN")));
        assertFalse(service.canAccessUrl("//evil.example.com/login", List.of("ROLE_ADMIN")));
        assertFalse(service.canAccessUrl("javascript:alert(1)", List.of("ROLE_ADMIN")));
    }
}
