package com.maharecruitment.gov.in.web.service.navigation.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.web.service.navigation.NavigationService;

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
                        "ROLE_HOD",
                        "ROLE_PM",
                        "ROLE_STM",
                        "ROLE_AGENCY",
                        "ROLE_USER");

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
                        case "HOD" -> "HOD";
                        case "COO" -> "COO";
                        default -> role.replace('_', ' ');
                };
        }
}
