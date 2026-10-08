package com.maharecruitment.gov.in.web.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmployeeTypeConversionForm {

    @NotNull(message = "Select an employee to convert.")
    private Long employeeId;

    @NotBlank(message = "Select a conversion direction.")
    private String targetType;

    private Long departmentId;

    private Long locationId;

    private Long cellId;

    private Long reportingHodUserId;

    private String managerType = "OTHER";

    private Long reportingManagerEmployeeId;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getCellId() {
        return cellId;
    }

    public void setCellId(Long cellId) {
        this.cellId = cellId;
    }

    public Long getReportingHodUserId() {
        return reportingHodUserId;
    }

    public void setReportingHodUserId(Long reportingHodUserId) {
        this.reportingHodUserId = reportingHodUserId;
    }

    public String getManagerType() {
        return managerType;
    }

    public void setManagerType(String managerType) {
        this.managerType = managerType;
    }

    public Long getReportingManagerEmployeeId() {
        return reportingManagerEmployeeId;
    }

    public void setReportingManagerEmployeeId(Long reportingManagerEmployeeId) {
        this.reportingManagerEmployeeId = reportingManagerEmployeeId;
    }
}
