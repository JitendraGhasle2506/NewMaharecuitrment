package com.maharecruitment.gov.in.department.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HrAgencyRankRowForm {

    private Long agencyId;

    private String categoryName;

    private Integer rankNumber;

    private java.util.List<String> mappedCategories;
}

