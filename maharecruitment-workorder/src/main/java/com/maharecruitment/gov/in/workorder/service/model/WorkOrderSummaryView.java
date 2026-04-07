package com.maharecruitment.gov.in.workorder.service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderStatus;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderSummaryView {

    private Long workOrderId;
    private String workOrderNumber;
    private WorkOrderType workOrderType;
    private WorkOrderStatus status;
    private String requestId;
    private String projectName;
    private String agencyName;
    private String departmentName;
    private LocalDate workOrderDate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer employeeCount;
    private boolean latestVersion;
    private String generatedByName;
    private LocalDateTime generatedOn;

    public boolean latestVersion() {
        return latestVersion;
    }
}
