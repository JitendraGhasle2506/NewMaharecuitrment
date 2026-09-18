package com.maharecruitment.gov.in.web.service.hr.model;

public record EmployeeProjectOptionView(
        Long projectId,
        String projectName,
        String projectCode,
        String scope,
        boolean active) {

    public String displayName() {
        return projectName + (projectCode == null || projectCode.isBlank() ? "" : " (" + projectCode + ")");
    }
}
