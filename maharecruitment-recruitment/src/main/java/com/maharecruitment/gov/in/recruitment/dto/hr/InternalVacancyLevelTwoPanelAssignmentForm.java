package com.maharecruitment.gov.in.recruitment.dto.hr;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyLevelTwoPanelAssignmentForm {

    private List<String> selectedRoleLabels = new ArrayList<>();

    @Size(min = 2, max = 5, message = "Select between 2 and 5 panel members.")
    private List<Long> selectedUserIds = new ArrayList<>();
}
