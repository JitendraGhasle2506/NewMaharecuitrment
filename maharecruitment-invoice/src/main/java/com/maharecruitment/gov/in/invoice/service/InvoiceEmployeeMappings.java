package com.maharecruitment.gov.in.invoice.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;

/** Shared identity rule for the loaded list and the billable employee lines. */
public final class InvoiceEmployeeMappings {
    private InvoiceEmployeeMappings() { }

    public static List<EmployeeProjectMappingEntity> uniqueEmployees(List<EmployeeProjectMappingEntity> mappings) {
        Map<Long, EmployeeProjectMappingEntity> employees = new LinkedHashMap<>();
        if (mappings != null) {
            for (EmployeeProjectMappingEntity mapping : mappings) {
                if (mapping != null && mapping.getEmployee() != null
                        && mapping.getEmployee().getEmployeeId() != null) {
                    employees.putIfAbsent(mapping.getEmployee().getEmployeeId(), mapping);
                }
            }
        }
        return List.copyOf(employees.values());
    }
}
