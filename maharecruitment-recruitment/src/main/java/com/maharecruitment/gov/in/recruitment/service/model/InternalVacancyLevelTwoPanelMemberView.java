package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyLevelTwoPanelMemberView {

    private Long panelUserId;
    private String panelMemberName;
    private String panelMemberDesignation;
}
