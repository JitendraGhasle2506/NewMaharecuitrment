package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;
import java.util.Map;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;

public interface ReportingManagerService {
    List<Map<String, Object>> getHodUsers();

    List<Map<String, Object>> getReportingAuthorities();
    
    List<Map<String, Object>> getManagersByType(String type);
    
    List<Map<String, Object>> getProjects();
    
    List<Map<String, Object>> getInternalEmployees(Long includeEmployeeId, Long hodUserId, String managerType);
    
    List<Map<String, Object>> getAllMappings();
    
    void saveMapping(Long hodUserId, String managerType, Long managerEmployeeId, Long projectId, List<Long> employeeIds);

    void updateMapping(Long mappingId, Long hodUserId, String managerType, Long managerEmployeeId,
            Long projectId, Long employeeId);

    Long resolveDirectReportingUserId(EmployeeReportingMappingEntity mapping);
}
