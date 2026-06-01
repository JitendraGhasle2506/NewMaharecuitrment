package com.maharecruitment.gov.in.web.service.dashboard.model;

public record ProjectWorkforceView(
        String code,
        String name,
        String cellName,
        int internal,
        int external,
        String status
) {
}
