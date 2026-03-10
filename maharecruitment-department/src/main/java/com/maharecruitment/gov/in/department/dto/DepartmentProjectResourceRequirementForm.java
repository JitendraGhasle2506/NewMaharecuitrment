package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentProjectResourceRequirementForm {

    @NotNull(message = "Designation is required.")
    private Long designationId;

    @NotBlank(message = "Designation name is required.")
    @Size(max = 200, message = "Designation name must not exceed 200 characters.")
    private String designationName;

    @NotBlank(message = "Level code is required.")
    @Size(max = 50, message = "Level code must not exceed 50 characters.")
    private String levelCode;

    @NotBlank(message = "Level name is required.")
    @Size(max = 100, message = "Level name must not exceed 100 characters.")
    private String levelName;

    @NotNull(message = "Monthly rate is required.")
    @DecimalMin(value = "0.01", message = "Monthly rate must be greater than zero.")
    private BigDecimal monthlyRate;

    @NotNull(message = "Required quantity is required.")
    @Min(value = 1, message = "Required quantity must be at least 1.")
    private Integer requiredQuantity;

    @NotNull(message = "Duration in months is required.")
    @Min(value = 3, message = "Duration in months must be at least 3.")
    private Integer durationInMonths;

    @NotNull(message = "Total cost is required.")
    @DecimalMin(value = "0.01", message = "Total cost must be greater than zero.")
    private BigDecimal totalCost;
}
