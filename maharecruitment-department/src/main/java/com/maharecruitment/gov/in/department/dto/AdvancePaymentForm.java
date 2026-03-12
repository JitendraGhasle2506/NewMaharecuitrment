package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;

@Getter
@Setter
@NoArgsConstructor
public class AdvancePaymentForm {

    private Long id;

    private DepartmentApplicationStatus applicationStatus;

    @NotNull(message = "Department project application reference is required")
    private Long departmentProjectApplicationId;

    @NotNull(message = "Department registration reference is required")
    private Long departmentRegistrationId;

    @NotBlank(message = "Proforma Invoice selection is required")
    private String proformaInvoiceId;

    @NotBlank(message = "Receipt number is required")
    private String receiptNumber;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal totalAmount;

    private String receiptOriginalName;

    private String receiptFileType;

    private MultipartFile receiptFile;

    private String remarks;
}
