package com.maharecruitment.gov.in.web.security.host;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "security")
public class HostProperties {

    private HostValidation hostValidation = new HostValidation();

    private List<String> allowedHosts = new ArrayList<>();

    private Set<Integer> allowedPorts = new LinkedHashSet<>();

    public HostValidation getHostValidation() {
        return hostValidation;
    }

    public void setHostValidation(HostValidation hostValidation) {
        this.hostValidation = hostValidation == null ? new HostValidation() : hostValidation;
    }

    boolean isHostValidationEnabled() {
        return hostValidation.isEnabled();
    }

    public List<String> getAllowedHosts() {
        return List.copyOf(allowedHosts);
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = new ArrayList<>();
        if (allowedHosts == null) {
            return;
        }

        allowedHosts.stream()
                .filter(StringUtils::hasText)
                .map(host -> host.trim().toLowerCase(Locale.ROOT))
                .forEach(this.allowedHosts::add);
    }

    public Set<Integer> getAllowedPorts() {
        return Set.copyOf(allowedPorts);
    }

    public void setAllowedPorts(Set<Integer> allowedPorts) {
        this.allowedPorts = new LinkedHashSet<>();
        if (allowedPorts == null) {
            return;
        }

        allowedPorts.stream()
                .filter(port -> port != null && port > 0 && port <= 65_535)
                .forEach(this.allowedPorts::add);
    }

    boolean isPortAllowed(int port) {
        return allowedPorts.contains(port);
    }

    public static class HostValidation {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
