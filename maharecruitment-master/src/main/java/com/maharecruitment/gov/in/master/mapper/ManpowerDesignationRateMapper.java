package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationRate;

@Component
public class ManpowerDesignationRateMapper {

    public ManpowerDesignationRateResponse toResponse(ManpowerDesignationRate entity) {
        return ManpowerDesignationRateResponse.builder()
                .rateId(entity.getRateId())
                .designationId(entity.getDesignationId())
                .levelCode(entity.getLevelCode())
                .grossMonthlyCtc(entity.getGrossMonthlyCtc())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .activeFlag(entity.getActiveFlag())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }
}

