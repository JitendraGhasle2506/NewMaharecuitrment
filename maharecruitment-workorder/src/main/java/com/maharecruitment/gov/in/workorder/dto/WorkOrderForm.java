package com.maharecruitment.gov.in.workorder.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkOrderForm {

    private Long parentWorkOrderId;

    private Long agencyId;

    private String recruitmentType = "EXTERNAL";

    @NotNull(message = "Work-order type is required.")
    private WorkOrderType workOrderType = WorkOrderType.NEW;

    @NotNull(message = "Work-order date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workOrderDate;

    @NotNull(message = "Effective-from date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    @NotNull(message = "Effective-to date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveTo;

    @Size(max = 100, message = "Reference number must be at most 100 characters.")
    private String referenceNumber;

    @Size(max = 500, message = "Subject line must be at most 500 characters.")
    private String subjectLine;

    @Size(max = 1000, message = "Additional instructions must be at most 1000 characters.")
    private String purposeSummary;

    @Size(max = 1500, message = "Extension reason must be at most 1500 characters.")
    private String extensionReason;

    @NotEmpty(message = "Select at least one employee.")
    private List<Long> employeeIds = new ArrayList<>();
}
