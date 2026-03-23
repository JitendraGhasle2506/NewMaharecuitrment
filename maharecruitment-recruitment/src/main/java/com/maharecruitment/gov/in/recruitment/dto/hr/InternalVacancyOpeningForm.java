package com.maharecruitment.gov.in.recruitment.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyOpeningForm {

    private Long internalVacancyOpeningId;

    @NotNull(message = "Project is required.")
    private Long projectId;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;

    @Valid
    @Size(min = 1, message = "Add at least one designation requirement.")
    private List<InternalVacancyRequirementForm> requirements = new ArrayList<>();
}
