package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.LocationMasterDto;
import com.maharecruitment.gov.in.master.entity.LocationMaster;

@Component
public class LocationMasterMapper {

    public LocationMasterDto toDto(LocationMaster entity) {
        if (entity == null) {
            return null;
        }
        return LocationMasterDto.builder()
                .locationId(entity.getLocationId())
                .departmentName(entity.getDepartmentName())
                .locationName(entity.getLocationName())
                .officeName(entity.getOfficeName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .radiusMeters(entity.getRadiusMeters())
                .activeFlag(entity.getActiveFlag())
                .createdUserId(entity.getCreatedUserId())
                .updatedUserId(entity.getUpdatedUserId())
                .createdDateTime(entity.getCreatedDateTime())
                .updatedDateTime(entity.getUpdatedDateTime())
                .build();
    }
}
