package com.maharecruitment.gov.in.web.service.navigation.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import com.maharecruitment.gov.in.auth.constant.CommonConstant;
import com.maharecruitment.gov.in.web.service.navigation.NavigationService;

@Service
public class RoleBasedNavigationService implements NavigationService {

        private static final String DEFAULT_HOME_URL = "/home";
        private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
        private static final Pattern ABSOLUTE_HTTP_URL = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
        private static final Pattern URI_SCHEME = Pattern.compile("^[a-z][a-z0-9+.-]*:", Pattern.CASE_INSENSITIVE);

        private static final List<String> ROLE_PRIORITY = List.of(
                        "ROLE_ADMIN",
                        "ROLE_HR",
                        "ROLE_AUDITOR",
                        "ROLE_MD",
                        "ROLE_COO",
                        "ROLE_DEPARTMENT",
                        "ROLE_EMPLOYEE",
                        "ROLE_HOD",
                        "ROLE_PM",
                        "ROLE_STM",
                        "ROLE_AGENCY",
                        "ROLE_USER");

        private static final List<AccessRule> ACCESS_RULES = List.of(
                        AccessRule.forRoles(Set.of("ROLE_ADMIN", "ROLE_HR"), "/common/mahait-profile",
                                        "/common/mahait-profile/**"),
                        AccessRule.forRoles(Set.of("ROLE_ADMIN", "ROLE_HR"), "/common/holidays",
                                        "/common/holidays/**"),
                        AccessRule.forAuthenticated("/home", "/common", "/common/**"),
                        AccessRule.forAuthenticated("/reporting-manager/attendance",
                                        "/reporting-manager/attendance/**"),
                        AccessRule.forRoles(Set.of("ROLE_ADMIN"), "/admin", "/admin/**"),
                        AccessRule.forRoles(Set.of("ROLE_HR", "ROLE_AUDITOR"), "/hr/department/payment",
                                        "/hr/department/payment/**"),
                        AccessRule.forRoles(Set.of("ROLE_HR"), "/hr", "/hr/**", "/employees", "/employees/**"),
                        AccessRule.forRoles(Set.of("ROLE_AGENCY"), "/agency", "/agency/**"),
                        AccessRule.forRoles(Set.of("ROLE_USER"), "/user", "/user/**"),
                        AccessRule.forRoles(Set.of("ROLE_COO", "ROLE_HOD", "ROLE_HOD1", "ROLE_STM", "ROLE_HR",
                                        "ROLE_PM", "ROLE_EMPLOYEE"), "/panel", "/panel/**"),
                        AccessRule.forRoles(Set.of("ROLE_HOD", "ROLE_PM", "ROLE_STM", "ROLE_EMPLOYEE"),
                                        "/interview-authority", "/interview-authority/**"),
                        AccessRule.forRoles(Set.of("ROLE_STM"), "/stm", "/stm/**"),
                        AccessRule.forRoles(Set.of("ROLE_PM"), "/pm", "/pm/**"),
                        AccessRule.forRoles(Set.of("ROLE_HOD"), "/hod1", "/hod1/**", "/hod2", "/hod2/**"),
                        AccessRule.forRoles(Set.of("ROLE_COO", "ROLE_AUDITOR"), "/coo", "/coo/**"),
                        AccessRule.forRoles(Set.of("ROLE_MD"), "/md", "/md/**"),
                        AccessRule.forRoles(Set.of("ROLE_EMPLOYEE"), "/employee", "/employee/**"),
                        AccessRule.forRoles(Set.of("ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR"),
                                        "/department/payment/*/receipt"),
                        AccessRule.forRoles(Set.of("ROLE_ADMIN", "ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR"),
                                        "/invoice", "/invoice/**"),
                        AccessRule.forRoles(Set.of("ROLE_DEPARTMENT"), "/department", "/department/**"),
                        AccessRule.forRoles(Set.of("ROLE_AUDITOR"), "/auditor", "/auditor/**"),
                        AccessRule.forRoles(Set.of("ROLE_ADMIN"),
                                        "/attendance", "/attendance/**",
                                        "/eservicebook", "/eservicebook/**",
                                        "/pension", "/pension/**",
                                        "/hrms", "/hrms/**",
                                        "/payroll", "/payroll/**"));

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

        @Override
        public boolean canAccessUrl(String url, List<String> roles) {
                String normalizedUrl = normalizeUrl(url);
                if (normalizedUrl.isBlank()) {
                        return false;
                }

                Set<String> resolvedRoles = new HashSet<>(orderRoles(roles));
                for (AccessRule accessRule : ACCESS_RULES) {
                        if (accessRule.matches(normalizedUrl)) {
                                return accessRule.allows(resolvedRoles);
                        }
                }

                return !resolvedRoles.isEmpty();
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
                        case "MD" -> "MD";
                        default -> role.replace('_', ' ');
                };
        }

        private static String normalizeUrl(String url) {
                if (url == null) {
                        return "";
                }

                String normalized = url.trim();
                if (normalized.isBlank() || "#".equals(normalized)) {
                        return "";
                }

                if (isExternalUrl(normalized)) {
                        return "";
                }

                int fragmentIndex = normalized.indexOf('#');
                if (fragmentIndex >= 0) {
                        normalized = normalized.substring(0, fragmentIndex);
                }

                int queryIndex = normalized.indexOf('?');
                if (queryIndex >= 0) {
                        normalized = normalized.substring(0, queryIndex);
                }

                if (!normalized.startsWith("/")) {
                        normalized = "/" + normalized;
                }

                while (normalized.length() > 1 && normalized.endsWith("/")) {
                        normalized = normalized.substring(0, normalized.length() - 1);
                }

                return normalized;
        }

        private static boolean isAbsoluteHttpUrl(String url) {
                return ABSOLUTE_HTTP_URL.matcher(url).find();
        }

        private static boolean isExternalUrl(String url) {
                return isAbsoluteHttpUrl(url) || url.startsWith("//") || URI_SCHEME.matcher(url).find();
        }

        private record AccessRule(List<String> patterns, Set<String> roles, boolean authenticatedOnly) {

                private static AccessRule forAuthenticated(String... patterns) {
                        return new AccessRule(List.of(patterns), Set.of(), true);
                }

                private static AccessRule forRoles(Set<String> roles, String... patterns) {
                        return new AccessRule(List.of(patterns), Set.copyOf(roles), false);
                }

                private boolean matches(String url) {
                        return patterns.stream().anyMatch(pattern -> matchesPattern(pattern, url));
                }

                private boolean allows(Set<String> userRoles) {
                        if (authenticatedOnly) {
                                return !userRoles.isEmpty();
                        }

                        return roles.stream().anyMatch(userRoles::contains);
                }

                private static boolean matchesPattern(String pattern, String url) {
                        if (PATH_MATCHER.match(pattern, url)) {
                                return true;
                        }

                        if (!pattern.endsWith("/**")) {
                                return false;
                        }

                        String basePath = pattern.substring(0, pattern.length() - 3);
                        return basePath.equals(url);
                }
        }

}
