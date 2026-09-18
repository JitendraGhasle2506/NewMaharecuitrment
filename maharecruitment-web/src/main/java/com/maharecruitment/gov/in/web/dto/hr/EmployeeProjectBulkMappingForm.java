package com.maharecruitment.gov.in.web.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProjectBulkMappingForm {

    @NotNull(message = "Select a target project.")
    private Long projectId;

    @NotEmpty(message = "Select at least one employee.")
    private List<Long> employeeIds = new ArrayList<>();
}
