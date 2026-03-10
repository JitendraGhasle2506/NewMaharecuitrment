package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorDepartmentSubDepartmentRequestView {

    private Long departmentId;
    private String departmentName;
    private List<AuditorSubDepartmentProjectCountView> subDepartmentProjectCounts;
}
