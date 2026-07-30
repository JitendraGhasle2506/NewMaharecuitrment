package com.maharecruitment.gov.in.web.properties;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Component
@ConfigurationProperties(prefix = "app.security.transport")
public class TransportSecurityProperties {

    private boolean requireHttps = true;

    private boolean allowLoopbackHttp = true;

    private boolean trustForwardedHeaders;

    private Integer httpPort;

    private Integer httpsPort;

    private Set<String> loopbackHosts = new LinkedHashSet<>(Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1"));

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public boolean isAllowLoopbackHttp() {
        return allowLoopbackHttp;
    }

    public void setAllowLoopbackHttp(boolean allowLoopbackHttp) {
        this.allowLoopbackHttp = allowLoopbackHttp;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public boolean hasPortMapping() {
        return httpPort != null && httpPort > 0 && httpsPort != null && httpsPort > 0;
    }

    public Set<String> getLoopbackHosts() {
        return Set.copyOf(loopbackHosts);
    }

    public void setLoopbackHosts(Set<String> loopbackHosts) {
        if (loopbackHosts == null || loopbackHosts.isEmpty()) {
            this.loopbackHosts = new LinkedHashSet<>();
            return;
        }

        this.loopbackHosts = new LinkedHashSet<>();
        loopbackHosts.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeHost)
                .forEach(this.loopbackHosts::add);
    }

    public boolean isHttpsRequiredFor(HttpServletRequest request) {
        return requireHttps && !isLoopbackHttpAllowed(request);
    }

    public boolean isLoopbackHttpAllowed(HttpServletRequest request) {
        return allowLoopbackHttp && !isSecureRequest(request) && isLoopbackRequest(request);
    }

    public boolean isLoopbackRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        return isLoopbackHost(request.getRemoteAddr())
                || isLoopbackHost(request.getLocalAddr());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request != null && request.isSecure();
    }

    private boolean isLoopbackHost(String host) {
        return StringUtils.hasText(host) && loopbackHosts.contains(normalizeHost(host));
    }

    private String normalizeHost(String host) {
        return host.trim().toLowerCase(Locale.ROOT);
    }
}
