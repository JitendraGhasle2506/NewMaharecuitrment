package com.maharecruitment.gov.in.workorder.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "work_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_order_number", columnNames = "work_order_number")
        },
        indexes = {
                @Index(name = "idx_work_order_number", columnList = "work_order_number"),
                @Index(name = "idx_work_order_parent", columnList = "parent_work_order_id"),
                @Index(name = "idx_work_order_root", columnList = "root_work_order_id"),
                @Index(name = "idx_work_order_request", columnList = "request_id"),
                @Index(name = "idx_work_order_agency", columnList = "agency_id"),
                @Index(name = "idx_work_order_status", columnList = "status"),
                @Index(name = "idx_work_order_date", columnList = "work_order_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "work_order_number", nullable = false, length = 60)
    private String workOrderNumber;

    @Column(name = "parent_work_order_id")
    private Long parentWorkOrderId;

    @Column(name = "root_work_order_id")
    private Long rootWorkOrderId;

    @Builder.Default
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Builder.Default
    @Column(name = "extension_sequence", nullable = false)
    private Integer extensionSequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_order_type", nullable = false, length = 20)
    private WorkOrderType workOrderType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status = WorkOrderStatus.GENERATED;

    @Column(name = "request_id", nullable = false, length = 50)
    private String requestId;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "project_code", length = 100)
    private String projectCode;

    @Column(name = "department_registration_id", nullable = false)
    private Long departmentRegistrationId;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(name = "sub_department_name", length = 200)
    private String subDepartmentName;

    @Column(name = "agency_id", nullable = false)
    private Long agencyId;

    @Column(name = "agency_name", nullable = false, length = 200)
    private String agencyName;

    @Column(name = "agency_contact_name", length = 150)
    private String agencyContactName;

    @Column(name = "agency_official_email", length = 255)
    private String agencyOfficialEmail;

    @Column(name = "agency_address", length = 500)
    private String agencyAddress;

    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;

    @Column(name = "subject_line", nullable = false, length = 500)
    private String subjectLine;

    @Column(name = "purpose_summary", length = 1000)
    private String purposeSummary;

    @Column(name = "extension_reason", length = 1500)
    private String extensionReason;

    @Column(name = "work_order_date", nullable = false)
    private LocalDate workOrderDate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "document_original_name", length = 255)
    private String documentOriginalName;

    @Column(name = "document_path", length = 700)
    private String documentPath;

    @Column(name = "document_content_type", length = 150)
    private String documentContentType;

    @Column(name = "document_file_size")
    private Long documentFileSize;

    @Builder.Default
    @Column(name = "employee_count", nullable = false)
    private Integer employeeCount = 0;

    @Builder.Default
    @Column(name = "latest_version", nullable = false)
    private Boolean latestVersion = Boolean.TRUE;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("employeeName ASC, employeeCode ASC")
    @Builder.Default
    private List<WorkOrderEmployeeMappingEntity> employeeMappings = new ArrayList<>();

    public void replaceEmployeeMappings(List<WorkOrderEmployeeMappingEntity> mappings) {
        employeeMappings.clear();
        if (mappings == null) {
            employeeCount = 0;
            return;
        }

        mappings.forEach(this::addEmployeeMapping);
        employeeCount = employeeMappings.size();
    }

    public void addEmployeeMapping(WorkOrderEmployeeMappingEntity mapping) {
        if (mapping == null) {
            return;
        }
        mapping.setWorkOrder(this);
        employeeMappings.add(mapping);
        employeeCount = employeeMappings.size();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        workOrderNumber = trim(workOrderNumber);
        requestId = trimUpper(requestId);
        projectName = trim(projectName);
        projectCode = trim(projectCode);
        departmentName = trim(departmentName);
        subDepartmentName = trim(subDepartmentName);
        agencyName = trim(agencyName);
        agencyContactName = trim(agencyContactName);
        agencyOfficialEmail = trimLower(agencyOfficialEmail);
        agencyAddress = trim(agencyAddress);
        referenceNumber = trim(referenceNumber);
        subjectLine = trim(subjectLine);
        purposeSummary = trim(purposeSummary);
        extensionReason = trim(extensionReason);
        documentOriginalName = trim(documentOriginalName);
        documentPath = trim(documentPath);
        documentContentType = trim(documentContentType);

        versionNumber = versionNumber == null || versionNumber < 1 ? 1 : versionNumber;
        extensionSequence = extensionSequence == null || extensionSequence < 0 ? 0 : extensionSequence;
        employeeCount = employeeCount == null || employeeCount < 0 ? 0 : employeeCount;
        latestVersion = !Boolean.FALSE.equals(latestVersion);
        active = !Boolean.FALSE.equals(active);
        if (status == null) {
            status = WorkOrderStatus.GENERATED;
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimUpper(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String trimLower(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
