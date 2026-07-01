package com.maharecruitment.gov.in.web.security.host;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class HostValidator {

    private static final int MAX_HOST_LENGTH = 253;
    private static final int MAX_HOST_HEADER_LENGTH = 261;
    private static final Pattern HOST_LABEL =
            Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern DECIMAL = Pattern.compile("\\d+");
    private static final Pattern IPV4_LIKE = Pattern.compile("[0-9.]+");

    private final HostProperties hostProperties;

    public HostValidator(HostProperties hostProperties) {
        this.hostProperties = hostProperties;
    }

    public HostValidationResult validate(Enumeration<String> hostHeaders) {
        return validate(hostHeaders == null ? List.of() : java.util.Collections.list(hostHeaders));
    }

    public HostValidationResult validate(List<String> hostHeaders) {
        if (hostHeaders == null || hostHeaders.isEmpty()) {
            return HostValidationResult.rejected("missing Host header");
        }
        if (hostHeaders.size() != 1) {
            return HostValidationResult.rejected("multiple Host headers");
        }
        return validate(hostHeaders.get(0));
    }

    public HostValidationResult validate(String hostHeader) {
        if (!StringUtils.hasText(hostHeader)) {
            return HostValidationResult.rejected("empty Host header");
        }
        if (hostHeader.length() > MAX_HOST_HEADER_LENGTH) {
            return HostValidationResult.rejected("Host header exceeds maximum length");
        }
        if (!hostHeader.equals(hostHeader.trim())) {
            return HostValidationResult.rejected("Host header contains whitespace");
        }
        if (containsUnsafeCharacter(hostHeader)) {
            return HostValidationResult.rejected("Host header contains unsafe characters");
        }
        if (hostHeader.contains(",") || hostHeader.contains("@") || hostHeader.contains("/") || hostHeader.contains("\\")) {
            return HostValidationResult.rejected("Host header contains invalid delimiters");
        }
        ParsedHost parsedHost = parseHost(hostHeader);
        if (!parsedHost.valid()) {
            return HostValidationResult.rejected(parsedHost.reason());
        }
        if (parsedHost.port() != null && !hostProperties.isPortAllowed(parsedHost.port())) {
            return HostValidationResult.rejected("Host header contains a port that is not allowed");
        }

        String host = parsedHost.host();
        HostValidationResult syntaxResult = validateHostSyntax(host);
        if (!syntaxResult.valid()) {
            return syntaxResult;
        }
        if (!allowedHosts().contains(host)) {
            return HostValidationResult.rejected("Host header is not allowlisted");
        }

        return HostValidationResult.allowed();
    }

    private ParsedHost parseHost(String hostHeader) {
        if (hostHeader.startsWith("[") || hostHeader.endsWith("]")) {
            return ParsedHost.invalid("IPv6 host literals are not allowed");
        }

        int firstColon = hostHeader.indexOf(':');
        if (firstColon < 0) {
            return ParsedHost.valid(hostHeader.toLowerCase(Locale.ROOT), null);
        }
        if (firstColon != hostHeader.lastIndexOf(':')) {
            return ParsedHost.invalid("Host header contains multiple port separators");
        }

        String host = hostHeader.substring(0, firstColon);
        String portValue = hostHeader.substring(firstColon + 1);
        if (!StringUtils.hasText(host) || !DECIMAL.matcher(portValue).matches()) {
            return ParsedHost.invalid("Host header contains an invalid port");
        }

        try {
            int port = Integer.parseInt(portValue);
            if (port <= 0 || port > 65_535) {
                return ParsedHost.invalid("Host header contains an invalid port");
            }
            return ParsedHost.valid(host.toLowerCase(Locale.ROOT), port);
        } catch (NumberFormatException ex) {
            return ParsedHost.invalid("Host header contains an invalid port");
        }
    }

    private HostValidationResult validateHostSyntax(String host) {
        if (!StringUtils.hasText(host)) {
            return HostValidationResult.rejected("Host header is empty");
        }
        if (host.length() > MAX_HOST_LENGTH) {
            return HostValidationResult.rejected("Host value exceeds RFC length");
        }
        if (host.endsWith(".")) {
            return HostValidationResult.rejected("Host header contains a trailing dot");
        }
        if (host.contains("..")) {
            return HostValidationResult.rejected("Host header contains an empty label");
        }
        if (host.startsWith("0x") || DECIMAL.matcher(host).matches()) {
            return HostValidationResult.rejected("Host header contains an IP address variation");
        }

        if (isCanonicalIpv4(host)) {
            return HostValidationResult.allowed();
        }
        if (IPV4_LIKE.matcher(host).matches()) {
            return HostValidationResult.rejected("Host header contains an invalid IP address");
        }
        if ("localhost".equals(host)) {
            return HostValidationResult.allowed();
        }
        if (!host.contains(".")) {
            return HostValidationResult.rejected("Host header is not a fully qualified domain name");
        }

        String[] labels = host.split("\\.", -1);
        for (String label : labels) {
            if (!HOST_LABEL.matcher(label).matches()) {
                return HostValidationResult.rejected("Host header contains an invalid domain label");
            }
        }

        String topLevelLabel = labels[labels.length - 1];
        if (topLevelLabel.length() < 2 || topLevelLabel.chars().noneMatch(Character::isLetter)) {
            return HostValidationResult.rejected("Host header contains an invalid top-level domain");
        }

        return HostValidationResult.allowed();
    }

    private boolean containsUnsafeCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current <= 31 || current == 127 || current > 127 || Character.isWhitespace(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCanonicalIpv4(String host) {
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (!DECIMAL.matcher(octet).matches()) {
                return false;
            }
            if (octet.length() > 1 && octet.startsWith("0")) {
                return false;
            }
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException ex) {
                return false;
            }
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private Set<String> allowedHosts() {
        Set<String> normalizedHosts = new HashSet<>();
        for (String allowedHost : hostProperties.getAllowedHosts()) {
            if (StringUtils.hasText(allowedHost)) {
                normalizedHosts.add(allowedHost.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalizedHosts;
    }

    private record ParsedHost(boolean valid, String host, Integer port, String reason) {

        private static ParsedHost valid(String host, Integer port) {
            return new ParsedHost(true, host, port, "valid");
        }

        private static ParsedHost invalid(String reason) {
            return new ParsedHost(false, null, null, reason);
        }
    }
}
