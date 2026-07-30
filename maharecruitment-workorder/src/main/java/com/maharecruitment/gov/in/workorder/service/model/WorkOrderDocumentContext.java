package com.maharecruitment.gov.in.workorder.service.model;

import java.time.LocalDate;
import java.util.List;

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
public class WorkOrderDocumentContext {

    private String workOrderNumber;
    private String parentWorkOrderNumber;
    private String referenceNumber;
    private WorkOrderType workOrderType;
    private LocalDate workOrderDate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String subjectLine;
    private String purposeSummary;
    private String extensionReason;
    private String requestId;
    private String projectName;
    private String projectCode;
    private String departmentName;
    private String subDepartmentName;
    private String agencyName;
    private String agencyContactName;
    private String agencyOfficialEmail;
    private String agencyAddress;
    private String issuedByOrganizationName;
    private String issuedByAddress;
    private List<WorkOrderEmployeeView> employees;

    public String workOrderNumber() { return workOrderNumber; }
    public String parentWorkOrderNumber() { return parentWorkOrderNumber; }
    public String referenceNumber() { return referenceNumber; }
    public WorkOrderType workOrderType() { return workOrderType; }
    public LocalDate workOrderDate() { return workOrderDate; }
    public LocalDate effectiveFrom() { return effectiveFrom; }
    public LocalDate effectiveTo() { return effectiveTo; }
    public String subjectLine() { return subjectLine; }
    public String purposeSummary() { return purposeSummary; }
    public String extensionReason() { return extensionReason; }
    public String requestId() { return requestId; }
    public String projectName() { return projectName; }
    public String projectCode() { return projectCode; }
    public String departmentName() { return departmentName; }
    public String subDepartmentName() { return subDepartmentName; }
    public String agencyName() { return agencyName; }
    public String agencyContactName() { return agencyContactName; }
    public String agencyOfficialEmail() { return agencyOfficialEmail; }
    public String agencyAddress() { return agencyAddress; }
    public String issuedByOrganizationName() { return issuedByOrganizationName; }
    public String issuedByAddress() { return issuedByAddress; }
    public List<WorkOrderEmployeeView> employees() { return employees; }
}
