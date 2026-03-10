package com.maharecruitment.gov.in.auth.util;

import java.util.Locale;

public final class AuthorityUtil {

    private AuthorityUtil() {
    }

    public static String toAuthority(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        String normalized = roleName.trim()
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
