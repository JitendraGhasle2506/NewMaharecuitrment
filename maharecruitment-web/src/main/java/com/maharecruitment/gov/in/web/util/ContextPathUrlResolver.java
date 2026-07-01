package com.maharecruitment.gov.in.web.util;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("contextPathUrl")
public class ContextPathUrlResolver {

    private static final Pattern ABSOLUTE_HTTP_URL = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHEME_RELATIVE_URL = Pattern.compile("^//");

    public String resolve(String contextPath, String url) {
        return resolve(contextPath, url, "#");
    }

    public String resolve(String contextPath, String url, String fallbackUrl) {
        if (!StringUtils.hasText(url)) {
            return resolveRequired(contextPath, fallbackUrl);
        }

        String trimmedUrl = url.trim();
        if ("#".equals(trimmedUrl) || isExternalUrl(trimmedUrl)) {
            return trimmedUrl;
        }

        String normalizedContextPath = normalizeContextPath(contextPath);
        String applicationPath = toApplicationPath(normalizedContextPath, trimmedUrl);
        return normalizedContextPath + applicationPath;
    }

    public String toRedirectPath(String contextPath, String url, String fallbackUrl) {
        String normalizedContextPath = normalizeContextPath(contextPath);
        String resolvedUrl = StringUtils.hasText(url) ? url.trim() : fallbackUrl;

        if (!StringUtils.hasText(resolvedUrl) || "#".equals(resolvedUrl) || isExternalUrl(resolvedUrl)) {
            return StringUtils.hasText(fallbackUrl) ? fallbackUrl : "/";
        }

        return toApplicationPath(normalizedContextPath, resolvedUrl);
    }

    private String resolveRequired(String contextPath, String fallbackUrl) {
        String resolvedFallback = StringUtils.hasText(fallbackUrl) ? fallbackUrl.trim() : "/";
        return resolve(contextPath, resolvedFallback, "/");
    }

    private String toApplicationPath(String contextPath, String url) {
        String normalizedPath = url.startsWith("/") ? url : "/" + url;

        while (StringUtils.hasText(contextPath) && startsWithContextSegment(normalizedPath, contextPath)) {
            normalizedPath = normalizedPath.substring(contextPath.length());
            if (!StringUtils.hasText(normalizedPath)) {
                normalizedPath = "/";
            } else if (normalizedPath.startsWith("?") || normalizedPath.startsWith("#")) {
                normalizedPath = "/" + normalizedPath;
            }
        }

        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }

    private boolean startsWithContextSegment(String path, String contextPath) {
        return path.equals(contextPath)
                || path.startsWith(contextPath + "/")
                || path.startsWith(contextPath + "?")
                || path.startsWith(contextPath + "#");
    }

    private String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }

        String normalized = contextPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isExternalUrl(String url) {
        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        return ABSOLUTE_HTTP_URL.matcher(normalizedUrl).find()
                || SCHEME_RELATIVE_URL.matcher(normalizedUrl).find();
    }
}
