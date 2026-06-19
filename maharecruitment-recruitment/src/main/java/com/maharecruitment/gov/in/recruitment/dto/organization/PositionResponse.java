package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
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
public class PositionResponse {

    private Long positionId;
    private String positionName;
    private Long projectId;
    private String projectName;
    private String projectCode;
    private Long teamId;
    private String teamName;
    private Long cellId;
    private String cellName;
    private Long designationId;
    private String designationName;
    private String levelCode;
    private String levelName;
    private Long reportingPositionId;
    private String reportingPositionName;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String displayName;
    private Integer displayOrder;
    private PositionStatus positionStatus;
    private OrganizationRecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
