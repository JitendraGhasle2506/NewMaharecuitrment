package com.maharecruitment.gov.in.web.service.hr.model;

import java.util.List;

public record EmployeeLocationMappingEditView(
        EmployeeLocationMappingEmployeeView employee,
        List<EmployeeLocationOptionView> availableLocations,
        List<EmployeeLocationOptionView> selectedLocations,
        List<EmployeeLocationMappingAuditView> auditLogs) {

    public EmployeeLocationOptionView primaryLocation() {
        return selectedLocations.stream()
                .filter(EmployeeLocationOptionView::primary)
                .findFirst()
                .orElse(null);
    }

    public List<EmployeeLocationOptionView> secondaryLocations() {
        return selectedLocations.stream()
                .filter(location -> !location.primary())
                .toList();
    }
}
