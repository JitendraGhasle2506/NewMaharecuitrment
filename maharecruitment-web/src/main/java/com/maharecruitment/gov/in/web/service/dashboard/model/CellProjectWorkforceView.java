package com.maharecruitment.gov.in.web.service.dashboard.model;

public record CellProjectWorkforceView(
        String cellName,
        int totalProjects,
        int internalProjects,
        int externalProjects,
        int internalEmployees,
        int externalEmployees
) {
    public int totalEmployees() {
        return internalEmployees + externalEmployees;
    }
}
