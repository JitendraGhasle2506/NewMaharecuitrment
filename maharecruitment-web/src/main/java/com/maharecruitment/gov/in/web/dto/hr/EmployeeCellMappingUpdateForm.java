package com.maharecruitment.gov.in.web.dto.hr;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeCellMappingUpdateForm {

    @NotNull(message = "Select a cell to map this employee.")
    private Long cellId;
}
