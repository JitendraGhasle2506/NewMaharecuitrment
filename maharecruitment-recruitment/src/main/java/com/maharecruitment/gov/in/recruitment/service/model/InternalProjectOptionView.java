package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalProjectOptionView {

    private Long projectId;
    private String projectName;
}
