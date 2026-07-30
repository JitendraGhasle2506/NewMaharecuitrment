package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.CommissionRateAuditLogResponse;
import com.maharecruitment.gov.in.master.dto.CommissionRateResponse;
import com.maharecruitment.gov.in.master.entity.CommissionRateAuditLog;
import com.maharecruitment.gov.in.master.entity.CommissionRateMaster;

@Component
public class CommissionRateMapper {

    public CommissionRateResponse toResponse(CommissionRateMaster entity) {
        return CommissionRateResponse.builder()
                .commissionRateId(entity.getCommissionRateId())
                .commissionCode(entity.getCommissionCode())
                .commissionPercentage(entity.getCommissionPercentage())
                .effectiveDate(entity.getEffectiveDate())
                .activeFlag(entity.getActiveFlag())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }

    public CommissionRateAuditLogResponse toAuditResponse(CommissionRateAuditLog entity) {
        return CommissionRateAuditLogResponse.builder()
                .auditId(entity.getAuditId())
                .commissionRateId(entity.getCommissionRateId())
                .actionType(entity.getActionType())
                .actorUserId(entity.getActorUserId())
                .actorUsername(entity.getActorUsername())
                .actionTimestamp(entity.getActionTimestamp())
                .details(entity.getDetails())
                .build();
    }
}
