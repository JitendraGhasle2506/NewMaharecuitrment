package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionBulkCreateResponse {

    private Long cellId;
    private String cellName;
    private String designationName;
    private String levelCode;
    private int createdCount;

    @Builder.Default
    private List<PositionResponse> positions = new ArrayList<>();
}
