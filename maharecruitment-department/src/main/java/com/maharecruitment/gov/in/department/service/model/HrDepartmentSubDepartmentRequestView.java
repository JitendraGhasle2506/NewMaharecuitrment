package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrDepartmentSubDepartmentRequestView {

    private Long departmentId;
    private String departmentName;
    private List<HrSubDepartmentProjectCountView> subDepartmentProjectCounts;
}
