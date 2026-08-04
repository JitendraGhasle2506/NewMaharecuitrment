package com.maharecruitment.gov.in.web.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeCellBulkMappingForm {

    @NotNull(message = "Select a cell before assigning employees.")
    private Long cellId;

    @NotEmpty(message = "Select at least one employee to assign.")
    private List<Long> employeeIds = new ArrayList<>();
}
