package com.maharecruitment.gov.in.workorder.service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.maharecruitment.gov.in.audit.dto.AuditEventView;
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
public class WorkOrderDetailView {

    private Long workOrderId;
    private String workOrderNumber;
    private String parentWorkOrderNumber;
    private Long parentWorkOrderId;
    private Long rootWorkOrderId;
    private Integer versionNumber;
    private Integer extensionSequence;
    private WorkOrderType workOrderType;
    private WorkOrderStatus status;
    private String requestId;
    private String projectName;
    private String projectCode;
    private String departmentName;
    private String subDepartmentName;
    private String agencyName;
    private String agencyContactName;
    private String agencyOfficialEmail;
    private String agencyAddress;
    private String referenceNumber;
    private String subjectLine;
    private String purposeSummary;
    private String extensionReason;
    private LocalDate workOrderDate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String documentOriginalName;
    private String documentContentType;
    private boolean documentAvailable;
    private Integer employeeCount;
    private boolean latestVersion;
    private String generatedByName;
    private String generatedByLoginId;
    private LocalDateTime generatedOn;
    private String updatedByName;
    private String updatedByLoginId;
    private LocalDateTime updatedOn;
    private List<WorkOrderEmployeeView> employees;
    private List<WorkOrderVersionView> versionHistory;
    private List<AuditEventView> auditTrail;

    public Long workOrderId() { return workOrderId; }
    public String workOrderNumber() { return workOrderNumber; }
    public String parentWorkOrderNumber() { return parentWorkOrderNumber; }
    public Long parentWorkOrderId() { return parentWorkOrderId; }
    public Long rootWorkOrderId() { return rootWorkOrderId; }
    public Integer versionNumber() { return versionNumber; }
    public Integer extensionSequence() { return extensionSequence; }
    public WorkOrderType workOrderType() { return workOrderType; }
    public WorkOrderStatus status() { return status; }
    public String requestId() { return requestId; }
    public String projectName() { return projectName; }
    public String projectCode() { return projectCode; }
    public String departmentName() { return departmentName; }
    public String subDepartmentName() { return subDepartmentName; }
    public String agencyName() { return agencyName; }
    public String agencyContactName() { return agencyContactName; }
    public String agencyOfficialEmail() { return agencyOfficialEmail; }
    public String agencyAddress() { return agencyAddress; }
    public String referenceNumber() { return referenceNumber; }
    public String subjectLine() { return subjectLine; }
    public String purposeSummary() { return purposeSummary; }
    public String extensionReason() { return extensionReason; }
    public LocalDate workOrderDate() { return workOrderDate; }
    public LocalDate effectiveFrom() { return effectiveFrom; }
    public LocalDate effectiveTo() { return effectiveTo; }
    public String documentOriginalName() { return documentOriginalName; }
    public String documentContentType() { return documentContentType; }
    public boolean documentAvailable() { return documentAvailable; }
    public Integer employeeCount() { return employeeCount; }
    public boolean latestVersion() { return latestVersion; }
    public String generatedByName() { return generatedByName; }
    public String generatedByLoginId() { return generatedByLoginId; }
    public LocalDateTime generatedOn() { return generatedOn; }
    public String updatedByName() { return updatedByName; }
    public String updatedByLoginId() { return updatedByLoginId; }
    public LocalDateTime updatedOn() { return updatedOn; }
    public List<WorkOrderEmployeeView> employees() { return employees; }
    public List<WorkOrderVersionView> versionHistory() { return versionHistory; }
    public List<AuditEventView> auditTrail() { return auditTrail; }
}
