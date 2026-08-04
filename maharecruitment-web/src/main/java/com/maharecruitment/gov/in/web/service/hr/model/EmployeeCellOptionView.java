package com.maharecruitment.gov.in.web.service.hr.model;

public record EmployeeCellOptionView(
        Long cellId,
        String cellName,
        Long wingId,
        String wingName,
        boolean active,
        String displayName) {
}
