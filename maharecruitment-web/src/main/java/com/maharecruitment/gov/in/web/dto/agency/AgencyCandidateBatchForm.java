package com.maharecruitment.gov.in.web.dto.agency;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyCandidateBatchForm {

    @NotNull(message = "Designation selection is required")
    private Long designationVacancyId;

    @Valid
    @NotEmpty(message = "Please add at least one candidate")
    private List<AgencyCandidateRowForm> candidates = new ArrayList<>();
}
