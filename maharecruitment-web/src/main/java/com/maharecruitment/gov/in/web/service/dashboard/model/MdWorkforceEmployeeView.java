package com.maharecruitment.gov.in.web.service.dashboard.model;

public record MdWorkforceEmployeeView(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String initials,
        String photoPath,
        String designationName,
        String levelCode,
        String positionName,
        String recruitmentType) {
}
