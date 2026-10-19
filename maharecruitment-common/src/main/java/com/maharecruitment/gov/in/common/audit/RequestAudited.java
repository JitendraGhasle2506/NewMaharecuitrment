package com.maharecruitment.gov.in.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditIpEntityListener.class)
@Getter
@Setter
public abstract class RequestAudited implements IpAudited {
    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;
}
