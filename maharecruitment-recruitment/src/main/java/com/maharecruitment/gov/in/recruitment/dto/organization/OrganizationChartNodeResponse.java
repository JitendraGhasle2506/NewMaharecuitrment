package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.util.ArrayList;
import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;

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
public class OrganizationChartNodeResponse {

    private String id;
    private String nodeType;
    private String label;
    private String subtitle;
    private Long cellId;
    private Long projectId;
    private Long teamId;
    private Long positionId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String designationName;
    private String levelCode;
    private String levelName;
    private PositionStatus positionStatus;
    private boolean vacant;
    private boolean expandable;

    @Builder.Default
    private List<OrganizationChartNodeResponse> children = new ArrayList<>();
}
