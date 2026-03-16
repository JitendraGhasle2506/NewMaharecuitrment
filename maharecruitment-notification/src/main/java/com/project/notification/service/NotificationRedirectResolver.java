package com.project.notification.service;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationRedirectResolver {

    public String resolve(String module, Long referenceId) {
        if (!StringUtils.hasText(module)) {
            return "/notifications";
        }

        String normalizedModule = module.trim().toUpperCase(Locale.ROOT);
        switch (normalizedModule) {
            case NotificationModules.RECRUITMENT:
                return referenceId != null ? "/recruitment/" + referenceId : "/recruitment";
            case NotificationModules.HR_DEPARTMENT_REQUESTS:
                return "/hr/department-requests";
            case NotificationModules.AUDITOR_DEPARTMENT_REQUESTS:
                return "/auditor/department-requests";
            case NotificationModules.DEPARTMENT_MANPOWER:
                return referenceId != null
                        ? "/department/manpower/" + referenceId + "/edit"
                        : "/department/manpower/list";
            default:
                return "/notifications";
        }
    }
}
