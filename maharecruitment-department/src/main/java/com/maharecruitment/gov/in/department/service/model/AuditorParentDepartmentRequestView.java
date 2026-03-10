package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorParentDepartmentRequestView {

    private Long departmentId;
    private String departmentName;
    private Long projectApplicationCount;
    private List<String> registeredSubDepartments;
    private List<AuditorSubDepartmentProjectCountView> subDepartmentProjectCounts;
}
