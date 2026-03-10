package com.maharecruitment.gov.in.web.service.navigation.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.web.service.navigation.NavigationService;
import com.maharecruitment.gov.in.web.service.navigation.model.SidebarItemView;

@Service
public class RoleBasedNavigationService implements NavigationService {

    private static final String DEFAULT_HOME_URL = "/home";

    private static final List<String> ROLE_PRIORITY = List.of(
            "ROLE_ADMIN",
            "ROLE_HR",
            "ROLE_AUDITOR",
            "ROLE_COO",
            "ROLE_DEPARTMENT",
            "ROLE_EMPLOYEE",
            "ROLE_HOD1",
            "ROLE_HOD2",
            "ROLE_PM",
            "ROLE_STM",
            "ROLE_AGENCY",
            "ROLE_USER",
            "ROLE_COMMON_MANAGER",
            "ROLE_ATTENDANCE_MANAGER",
            "ROLE_ESERVICEBOOK_MANAGER",
            "ROLE_PENSION_MANAGER",
            "ROLE_HRMS_MANAGER",
            "ROLE_PAYROLL_MANAGER"
    );

    private static final Map<String, List<SidebarItemView>> NAV_ITEMS_BY_ROLE = createNavItemsByRole();

    @Override
    public String resolveHomeUrl(List<String> roles) {
        Map<String, String> dashboardUrls = CommonConstant.getDashboardUrls();

        for (String role : orderRoles(roles)) {
            String path = dashboardUrls.get(role);
            if (path != null && !path.isBlank()) {
                return path;
            }
        }

        return DEFAULT_HOME_URL;
    }

    @Override
    public List<SidebarItemView> resolveSidebarItems(List<String> roles) {
        Map<String, SidebarItemView> uniqueItems = new LinkedHashMap<>();

        for (String role : orderRoles(roles)) {
            List<SidebarItemView> items = NAV_ITEMS_BY_ROLE.get(role);
            if (items == null || items.isEmpty()) {
                continue;
            }

            for (SidebarItemView item : items) {
                uniqueItems.putIfAbsent(item.url(), item);
            }
        }

        if (uniqueItems.isEmpty()) {
            String homeUrl = resolveHomeUrl(roles);
            uniqueItems.put(homeUrl, new SidebarItemView("Dashboard", "fa fa-gauge-high", homeUrl));
        }

        return List.copyOf(uniqueItems.values());
    }

    @Override
    public String resolvePrimaryRoleLabel(List<String> roles) {
        for (String role : orderRoles(roles)) {
            return toDisplayRole(role);
        }
        return "User";
    }

    private static List<String> orderRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        Set<String> incomingRoles = new LinkedHashSet<>(roles);
        List<String> orderedRoles = new ArrayList<>();

        for (String role : ROLE_PRIORITY) {
            if (incomingRoles.remove(role)) {
                orderedRoles.add(role);
            }
        }

        orderedRoles.addAll(incomingRoles);
        return orderedRoles;
    }

    private static String toDisplayRole(String authority) {
        if (authority == null || !authority.startsWith("ROLE_")) {
            return "User";
        }

        String role = authority.substring(5);
        return switch (role) {
            case "HOD1" -> "HOD-1";
            case "HOD2" -> "HOD-2";
            case "COO" -> "COO";
            case "HRMS_MANAGER" -> "HRMS Manager";
            case "PAYROLL_MANAGER" -> "Payroll Manager";
            case "COMMON_MANAGER" -> "Common Manager";
            case "ATTENDANCE_MANAGER" -> "Attendance Manager";
            case "ESERVICEBOOK_MANAGER" -> "E-Service Book Manager";
            case "PENSION_MANAGER" -> "Pension Manager";
            default -> role.replace('_', ' ');
        };
    }

    private static Map<String, List<SidebarItemView>> createNavItemsByRole() {
        Map<String, List<SidebarItemView>> navItems = new LinkedHashMap<>();

        navItems.put("ROLE_ADMIN", List.of(
                new SidebarItemView("Admin Dashboard", "fa fa-shield-halved", "/admin/dashboard"),
                new SidebarItemView("Role Menu Mapping", "fa fa-diagram-project", "/admin/role-menu-mappings"),
                new SidebarItemView("Menu Management", "fa fa-bars", "/admin/menus"),
                new SidebarItemView("Submenu Management", "fa fa-sitemap", "/admin/submenus"),
                new SidebarItemView("HR Dashboard", "fa fa-users", "/hr/dashboard"),
                new SidebarItemView("Common Module", "fa fa-layer-group", "/common"),
                new SidebarItemView("Attendance", "fa fa-calendar-check", "/attendance"),
                new SidebarItemView("E-Service Book", "fa fa-book", "/eservicebook"),
                new SidebarItemView("Pension", "fa fa-file-invoice-dollar", "/pension"),
                new SidebarItemView("HRMS", "fa fa-people-group", "/hrms"),
                new SidebarItemView("Payroll", "fa fa-money-check-dollar", "/payroll")
        ));

        navItems.put("ROLE_HR", List.of(
                new SidebarItemView("HR Dashboard", "fa fa-users", "/hr/dashboard")
        ));

        navItems.put("ROLE_USER", List.of(
                new SidebarItemView("User Dashboard", "fa fa-user", "/user/dashboard")
        ));

        navItems.put("ROLE_AGENCY", List.of(
                new SidebarItemView("Agency Dashboard", "fa fa-briefcase", "/agency/dashboard")
        ));

        navItems.put("ROLE_STM", List.of(
                new SidebarItemView("STM Dashboard", "fa fa-sitemap", "/stm/dashboard")
        ));

        navItems.put("ROLE_PM", List.of(
                new SidebarItemView("PM Dashboard", "fa fa-diagram-project", "/pm/dashboard")
        ));

        navItems.put("ROLE_HOD1", List.of(
                new SidebarItemView("HOD-1 Dashboard", "fa fa-user-tie", "/hod1/dashboard")
        ));

        navItems.put("ROLE_HOD2", List.of(
                new SidebarItemView("HOD-2 Dashboard", "fa fa-user-gear", "/hod2/dashboard")
        ));

        navItems.put("ROLE_COO", List.of(
                new SidebarItemView("COO Dashboard", "fa fa-chart-line", "/coo/dashboard")
        ));

        navItems.put("ROLE_AUDITOR", List.of(
                new SidebarItemView("Auditor Dashboard", "fa fa-magnifying-glass-chart", "/coo/dashboard")
        ));

        navItems.put("ROLE_DEPARTMENT", List.of(
                new SidebarItemView("Department Home", "fa fa-building", "/department/home")
        ));

        navItems.put("ROLE_EMPLOYEE", List.of(
                new SidebarItemView("Employee Dashboard", "fa fa-id-badge", "/employee/dashboard")
        ));

        navItems.put("ROLE_COMMON_MANAGER", List.of(
                new SidebarItemView("Common Module", "fa fa-layer-group", "/common")
        ));

        navItems.put("ROLE_ATTENDANCE_MANAGER", List.of(
                new SidebarItemView("Attendance Module", "fa fa-calendar-check", "/attendance")
        ));

        navItems.put("ROLE_ESERVICEBOOK_MANAGER", List.of(
                new SidebarItemView("E-Service Book", "fa fa-book", "/eservicebook")
        ));

        navItems.put("ROLE_PENSION_MANAGER", List.of(
                new SidebarItemView("Pension Module", "fa fa-file-invoice-dollar", "/pension")
        ));

        navItems.put("ROLE_HRMS_MANAGER", List.of(
                new SidebarItemView("HRMS Module", "fa fa-people-group", "/hrms")
        ));

        navItems.put("ROLE_PAYROLL_MANAGER", List.of(
                new SidebarItemView("Payroll Module", "fa fa-money-check-dollar", "/payroll")
        ));

        return Map.copyOf(navItems);
    }
}
