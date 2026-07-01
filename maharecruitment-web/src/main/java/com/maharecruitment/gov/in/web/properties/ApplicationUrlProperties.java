package com.maharecruitment.gov.in.web.properties;

import java.net.URI;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationUrlProperties {

    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            this.baseUrl = null;
            return;
        }

        URI uri = URI.create(baseUrl.trim());
        String scheme = uri.getScheme();
        if (!StringUtils.hasText(scheme)
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || !StringUtils.hasText(uri.getHost())
                || StringUtils.hasText(uri.getQuery())
                || StringUtils.hasText(uri.getFragment())) {
            throw new IllegalArgumentException("app.base-url must be an absolute HTTP(S) URL without query or fragment.");
        }

        String normalized = uri.toString();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.baseUrl = normalized.toLowerCase(Locale.ROOT).startsWith("http") ? normalized : uri.toString();
    }
}
