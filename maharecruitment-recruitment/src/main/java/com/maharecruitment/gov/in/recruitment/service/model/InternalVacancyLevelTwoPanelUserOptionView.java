package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoPanelUserOptionView {

    private Long userId;
    private String name;
    private String email;
    private String mobileNo;
    private String displayLabel;
    private String roleLabelCsv;
    private List<String> roleLabels;
}
