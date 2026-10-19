package com.maharecruitment.gov.in.common.audit;

import jakarta.persistence.PrePersist;

public class AuditIpEntityListener {
    @PrePersist
    public void capture(Object entity) {
        if (entity instanceof IpAudited audit) {
            audit.setIpAddress(AuditRequestContext.ipAddress(audit.getIpAddress()));
        }
    }
}
