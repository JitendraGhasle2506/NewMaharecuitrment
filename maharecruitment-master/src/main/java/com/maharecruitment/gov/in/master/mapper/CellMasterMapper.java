package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.CellMasterDto;
import com.maharecruitment.gov.in.master.entity.CellMaster;

@Component
public class CellMasterMapper {

    public CellMasterDto toDto(CellMaster entity) {
        if (entity == null) {
            return null;
        }
        return CellMasterDto.builder()
                .cellId(entity.getCellId())
                .cellName(entity.getCellName())
                .activeFlag(entity.getActiveFlag())
                .createdUserId(entity.getCreatedUserId())
                .updatedUserId(entity.getUpdatedUserId())
                .createdDateTime(entity.getCreatedDateTime())
                .updatedDateTime(entity.getUpdatedDateTime())
                .build();
    }
}
