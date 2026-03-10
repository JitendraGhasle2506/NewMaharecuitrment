package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorSubDepartmentProjectCountView {

    private Long subDepartmentId;
    private String subDepartmentName;
    private Long projectApplicationCount;
}
