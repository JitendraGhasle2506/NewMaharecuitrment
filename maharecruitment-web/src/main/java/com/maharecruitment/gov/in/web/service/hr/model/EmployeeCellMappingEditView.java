package com.maharecruitment.gov.in.web.service.hr.model;

import java.util.List;

public record EmployeeCellMappingEditView(
        EmployeeCellMappingEmployeeView employee,
        List<EmployeeCellOptionView> availableCells,
        EmployeeCellOptionView selectedCell,
        List<EmployeeCellMappingAuditView> auditLogs) {
}
