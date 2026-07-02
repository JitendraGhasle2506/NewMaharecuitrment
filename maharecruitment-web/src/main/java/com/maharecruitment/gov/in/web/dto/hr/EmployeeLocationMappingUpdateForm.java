package com.maharecruitment.gov.in.web.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeLocationMappingUpdateForm {

    @NotEmpty(message = "Select at least one employee location.")
    private List<Long> selectedLocationIds = new ArrayList<>();
}
