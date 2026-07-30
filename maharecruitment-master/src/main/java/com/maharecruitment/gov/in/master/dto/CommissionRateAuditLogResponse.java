package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.master.entity.CommissionRateAuditAction;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommissionRateAuditLogResponse {

    private Long auditId;
    private Long commissionRateId;
    private CommissionRateAuditAction actionType;
    private Long actorUserId;
    private String actorUsername;
    private LocalDateTime actionTimestamp;
    private String details;
}
