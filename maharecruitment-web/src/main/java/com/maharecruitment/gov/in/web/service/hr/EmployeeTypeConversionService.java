package com.maharecruitment.gov.in.web.service.hr;

public interface EmployeeTypeConversionService {

    ConversionResult convert(ConversionCommand command);

    java.util.List<ConversionAuditView> recentAudits();

    record ConversionCommand(
            Long employeeId,
            String targetType,
            Long departmentId,
            Long locationId,
            Long cellId,
            Long reportingHodUserId,
            String managerType,
            Long reportingManagerEmployeeId,
            String actorLoginId) {
    }

    record ConversionResult(
            String employeeName,
            String sourceType,
            String targetType,
            String departmentName,
            String locationName) {
    }

    record ConversionAuditView(
            String employeeName,
            String employeeCode,
            String sourceType,
            String targetType,
            String departmentName,
            String locationName,
            String cellName,
            String reportingHodName,
            String reportingManagerName,
            String actorLoginId,
            java.time.LocalDateTime occurredAt) {
    }
}
