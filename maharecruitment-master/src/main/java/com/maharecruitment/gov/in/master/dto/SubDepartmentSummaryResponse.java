package com.maharecruitment.gov.in.master.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubDepartmentSummaryResponse {

    private Long subDeptId;
    private String subDeptName;
}
