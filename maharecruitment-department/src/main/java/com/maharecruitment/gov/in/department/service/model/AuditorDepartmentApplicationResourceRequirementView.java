package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorDepartmentApplicationResourceRequirementView {

    private String designationName;
    private String levelName;
    private BigDecimal monthlyRate;
    private Integer requiredQuantity;
    private Integer durationInMonths;
    private BigDecimal totalCost;
}
