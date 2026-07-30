package com.maharecruitment.gov.in.web.dto.verification;

public enum VerificationChannel {
    EMAIL,
    SMS,
    BOTH,
    @Deprecated
    MOBILE;

    public VerificationChannel canonical() {
        return this == MOBILE ? SMS : this;
    }

    public boolean isSmsDelivery() {
        return this == SMS || this == MOBILE;
    }

    public boolean includesEmail() {
        return this == EMAIL || this == BOTH;
    }

    public boolean includesSms() {
        return this == SMS || this == MOBILE || this == BOTH;
    }
}
