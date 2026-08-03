package com.maharecruitment.gov.in.attendance.dto;

import java.math.BigDecimal;

import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceLocationDTO {

    private final Long locationId;
    private final String officeName;
    private final String locationName;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final Integer radiusMeters;
    private final boolean primary;

    public String getDisplayName() {
        String normalizedLocationName = StringUtils.hasText(locationName) ? locationName.trim() : "-";
        return StringUtils.hasText(officeName)
                ? officeName.trim() + " - " + normalizedLocationName
                : normalizedLocationName;
    }
}
