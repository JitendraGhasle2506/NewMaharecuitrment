package com.maharecruitment.gov.in.attendance.service.model;

import lombok.Data;

@Data
public class InternalAttendanceReportFilter {

    private Long agencyId;
    private Long departmentRegistrationId;
    private Long subDepartmentId;
    private Long projectId;
    private String employeeStatus;
    private String searchText;
    private Integer month;
    private Integer year;
}
