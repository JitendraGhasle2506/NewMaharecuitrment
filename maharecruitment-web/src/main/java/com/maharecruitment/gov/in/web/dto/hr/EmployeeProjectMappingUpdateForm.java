package com.maharecruitment.gov.in.web.dto.hr;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProjectMappingUpdateForm {

    @NotNull(message = "Select a project.")
    private Long projectId;
}
