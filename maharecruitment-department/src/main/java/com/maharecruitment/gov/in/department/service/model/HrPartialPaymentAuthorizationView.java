package com.maharecruitment.gov.in.department.service.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrPartialPaymentAuthorizationView {
    private Long departmentProjectApplicationId;
    private String requestId;
    private String projectName;
    private String departmentName;
    private String subDepartmentName;
    private BigDecimal totalEstimatedCost;
    private boolean partialPaymentAllowed;
}
