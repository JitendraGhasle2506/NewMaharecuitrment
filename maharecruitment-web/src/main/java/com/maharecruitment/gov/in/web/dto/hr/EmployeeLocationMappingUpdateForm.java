package com.maharecruitment.gov.in.web.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeLocationMappingUpdateForm {

    @NotEmpty(message = "Select at least one employee location.")
    private List<Long> selectedLocationIds = new ArrayList<>();

    @NotNull(message = "Designate a primary location.")
    private Long primaryLocationId;
}
