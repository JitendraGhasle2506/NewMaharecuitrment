package com.maharecruitment.gov.in.auth.util;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InternalNavigationUrlValidator {

    private static final Pattern URI_SCHEME = Pattern.compile("^[a-z][a-z0-9+.-]*:", Pattern.CASE_INSENSITIVE);

    public String normalizeRequiredApplicationPath(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " is required.");
        }

        String normalized = value.trim();
        if (containsUnsafeCharacter(normalized)
                || normalized.startsWith("//")
                || normalized.contains("\\")
                || URI_SCHEME.matcher(normalized).find()) {
            throw new IllegalArgumentException(label + " must be an application-relative path.");
        }

        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private boolean containsUnsafeCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current <= 31 || current == 127) {
                return true;
            }
        }
        return false;
    }
}
