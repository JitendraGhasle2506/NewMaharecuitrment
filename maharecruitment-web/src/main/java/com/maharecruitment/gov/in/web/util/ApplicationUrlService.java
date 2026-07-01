package com.maharecruitment.gov.in.web.util;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.properties.ApplicationUrlProperties;

@Service
public class ApplicationUrlService {

    private final ApplicationUrlProperties applicationUrlProperties;

    public ApplicationUrlService(ApplicationUrlProperties applicationUrlProperties) {
        this.applicationUrlProperties = applicationUrlProperties;
    }

    public String absoluteUrl(String applicationPath) {
        String baseUrl = applicationUrlProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("app.base-url is required for absolute URL generation.");
        }

        String normalizedPath = StringUtils.hasText(applicationPath) ? applicationPath.trim() : "/";
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return baseUrl + normalizedPath;
    }
}
