package com.maharecruitment.gov.in.web.service.hr.model;

import java.util.List;

public record EmployeeProjectMappingEditView(
        EmployeeProjectMappingEmployeeView employee,
        List<EmployeeProjectOptionView> availableProjects,
        EmployeeProjectOptionView selectedProject,
        String requiredScope) {
}
