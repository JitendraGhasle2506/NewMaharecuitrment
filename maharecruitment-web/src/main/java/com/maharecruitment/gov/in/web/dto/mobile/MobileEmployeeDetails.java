package com.maharecruitment.gov.in.web.dto.mobile;

public record MobileEmployeeDetails(
        Long empId,
        String employeeCode,
        String employeeName,
        String photoUrl,
        Long designationId,
        String designationName,
        Long departmentId,
        String departmentName,
        Long subDepartmentId,
        String subDepartmentName,
        String employeeType,
        Long reportingManagerId,
        String reportingManagerName,
        Long reportingDepartmentId,
        String reportingDepartmentName) {

    public static MobileEmployeeDetails empty() {
        return new MobileEmployeeDetails(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
