package com.maharecruitment.gov.in.web.service.dashboard.model;

public record ProjectWorkforceView(
        String code,
        String name,
        int internal,
        int external,
        String status
) {
}
