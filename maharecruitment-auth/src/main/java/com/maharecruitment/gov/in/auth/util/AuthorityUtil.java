package com.maharecruitment.gov.in.auth.util;

public final class AuthorityUtil {

    private AuthorityUtil() {
    }

    public static String toAuthority(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
    }
}
