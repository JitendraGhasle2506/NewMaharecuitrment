package com.maharecruitment.gov.in.web.security.host;

public record HostValidationResult(boolean valid, String reason) {

    public static HostValidationResult allowed() {
        return new HostValidationResult(true, "valid");
    }

    public static HostValidationResult rejected(String reason) {
        return new HostValidationResult(false, reason);
    }
}
