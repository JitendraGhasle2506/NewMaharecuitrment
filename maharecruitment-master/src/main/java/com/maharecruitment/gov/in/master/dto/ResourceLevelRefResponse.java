package com.maharecruitment.gov.in.master.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceLevelRefResponse {

    private Long levelId;
    private String levelCode;
    private String levelName;
}

