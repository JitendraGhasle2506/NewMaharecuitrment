package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.WingMasterDto;
import com.maharecruitment.gov.in.master.entity.WingMaster;

@Component
public class WingMasterMapper {

    public WingMasterDto toDto(WingMaster entity) {
        if (entity == null) {
            return null;
        }
        return WingMasterDto.builder()
                .wingId(entity.getWingId())
                .wingName(entity.getWingName())
                .activeFlag(entity.getActiveFlag())
                .createdUserId(entity.getCreatedUserId())
                .updatedUserId(entity.getUpdatedUserId())
                .createdDateTime(entity.getCreatedDateTime())
                .updatedDateTime(entity.getUpdatedDateTime())
                .build();
    }
}
