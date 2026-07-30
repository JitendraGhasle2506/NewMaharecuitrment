package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.DesignationCategoryMasterResponse;
import com.maharecruitment.gov.in.master.entity.DesignationCategoryMaster;

@Component
public class DesignationCategoryMasterMapper {

    public DesignationCategoryMasterResponse toResponse(DesignationCategoryMaster entity) {
        if (entity == null) {
            return null;
        }
        return DesignationCategoryMasterResponse.builder()
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .activeFlag(entity.getActiveFlag())
                .createdUserId(entity.getCreatedUserId())
                .updatedUserId(entity.getUpdatedUserId())
                .createdDateTime(entity.getCreatedDateTime())
                .updatedDateTime(entity.getUpdatedDateTime())
                .build();
    }
}
