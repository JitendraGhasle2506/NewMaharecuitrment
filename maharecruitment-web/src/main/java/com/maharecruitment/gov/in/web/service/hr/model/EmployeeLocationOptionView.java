package com.maharecruitment.gov.in.web.service.hr.model;

import java.math.BigDecimal;

public record EmployeeLocationOptionView(
        Long locationId,
        String officeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        String displayName,
        boolean primary) {
}
