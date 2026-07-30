package com.maharecruitment.gov.in.workorder.service.model;

import java.util.List;

import com.maharecruitment.gov.in.workorder.dto.WorkOrderForm;

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
public class WorkOrderFormView {

    private String pageTitle;
    private String pageSubtitle;
    private boolean extensionMode;
    private Long selectedAgencyId;
    private String selectedRecruitmentType;
    private WorkOrderForm form;
    private WorkOrderSummaryView parentWorkOrder;
    private List<WorkOrderAgencyOptionView> agencyOptions;
    private List<EmployeeWorkOrderOptionView> employeeOptions;

    public boolean extensionMode() {
        return extensionMode;
    }

    public WorkOrderForm form() {
        return form;
    }

    public String pageTitle() {
        return pageTitle;
    }

    public String pageSubtitle() {
        return pageSubtitle;
    }

    public WorkOrderSummaryView parentWorkOrder() {
        return parentWorkOrder;
    }

    public Long selectedAgencyId() {
        return selectedAgencyId;
    }

    public String selectedRecruitmentType() {
        return selectedRecruitmentType;
    }

    public List<WorkOrderAgencyOptionView> agencyOptions() {
        return agencyOptions;
    }

    public List<EmployeeWorkOrderOptionView> employeeOptions() {
        return employeeOptions;
    }
}
