package com.maharecruitment.gov.in.workorder.service.model;

import java.time.LocalDate;

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
public class WorkOrderVersionView {

    private Long workOrderId;
    private String workOrderNumber;
    private WorkOrderType workOrderType;
    private Integer versionNumber;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean latestVersion;

    public boolean latestVersion() {
        return latestVersion;
    }
}
