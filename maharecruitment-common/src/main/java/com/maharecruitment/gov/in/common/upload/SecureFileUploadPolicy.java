package com.maharecruitment.gov.in.common.upload;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecureFileUploadPolicy {

    private final String context;
    private final Set<String> allowedExtensions;

    private SecureFileUploadPolicy(String context, Set<String> allowedExtensions) {
        this.context = context == null || context.isBlank() ? "upload" : context.trim();
        this.allowedExtensions = allowedExtensions;
    }

    public static SecureFileUploadPolicy allowedExtensions(String context, Collection<String> allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            throw new IllegalArgumentException("At least one upload extension must be allowed.");
        }

        Set<String> normalizedExtensions = allowedExtensions.stream()
                .filter(extension -> extension != null && !extension.isBlank())
                .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                .map(extension -> extension.startsWith(".") ? extension.substring(1) : extension)
                .collect(Collectors.toUnmodifiableSet());
        if (normalizedExtensions.isEmpty()) {
            throw new IllegalArgumentException("At least one upload extension must be allowed.");
        }

        return new SecureFileUploadPolicy(context, normalizedExtensions);
    }

    public String context() {
        return context;
    }

    public Set<String> allowedExtensions() {
        return allowedExtensions;
    }
}
