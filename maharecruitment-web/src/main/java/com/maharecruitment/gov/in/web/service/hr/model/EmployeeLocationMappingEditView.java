package com.maharecruitment.gov.in.web.service.hr.model;

import java.util.List;

public record EmployeeLocationMappingEditView(
        EmployeeLocationMappingEmployeeView employee,
        List<EmployeeLocationOptionView> availableLocations,
        List<EmployeeLocationOptionView> selectedLocations,
        List<EmployeeLocationMappingAuditView> auditLogs) {
}
