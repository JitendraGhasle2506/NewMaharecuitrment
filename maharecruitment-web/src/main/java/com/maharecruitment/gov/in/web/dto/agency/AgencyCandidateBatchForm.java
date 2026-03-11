package com.maharecruitment.gov.in.web.dto.agency;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyCandidateBatchForm {

    private Long designationVacancyId;

    private List<AgencyCandidateRowForm> candidates = new ArrayList<>();
}
