package com.maharecruitment.gov.in.invoice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEmployeeType;

@Getter
@Setter
public class AgencyMonthlyBillGenerateRequest {

    @NotNull(message = "Agency is required")
    private Long agencyId;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid")
    @Max(value = 2100, message = "Year must be valid")
    private Integer year;

    @NotNull(message = "Employee type is required")
    private AgencyMonthlyBillEmployeeType employeeType = AgencyMonthlyBillEmployeeType.ALL;
}
