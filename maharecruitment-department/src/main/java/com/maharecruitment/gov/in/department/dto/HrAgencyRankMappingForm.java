package com.maharecruitment.gov.in.department.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HrAgencyRankMappingForm {

    private List<HrAgencyRankRowForm> rankRows = new ArrayList<>();
}

