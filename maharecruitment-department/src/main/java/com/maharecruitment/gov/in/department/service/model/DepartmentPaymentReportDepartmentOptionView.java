package com.maharecruitment.gov.in.department.service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DepartmentPaymentReportDepartmentOptionView {

    private Long departmentRegistrationId;
    private String departmentName;

    public DepartmentPaymentReportDepartmentOptionView(Long departmentRegistrationId, String departmentName) {
        this.departmentRegistrationId = departmentRegistrationId;
        this.departmentName = departmentName;
    }
}
