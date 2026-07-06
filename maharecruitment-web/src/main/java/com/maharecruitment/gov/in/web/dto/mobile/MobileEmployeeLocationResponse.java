package com.maharecruitment.gov.in.web.dto.mobile;

import java.math.BigDecimal;
import java.util.List;

public record MobileEmployeeLocationResponse(
        boolean success,
        String message,
        Long employeeId,
        List<Location> locations) {

    public MobileEmployeeLocationResponse {
        locations = locations == null ? List.of() : List.copyOf(locations);
    }

    public record Location(
            Long locationId,
            String officeName,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radiusMeters,
            String displayName) {
    }
}
