package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;

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
public class EmployeeTeamMappingResponse {

    private Long mappingId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long teamId;
    private String teamName;
    private Long projectId;
    private String projectName;
    private Long cellId;
    private String cellName;
    private Long positionId;
    private String positionName;
    private String designationName;
    private String levelCode;
    private String levelName;
    private LocalDate effectiveDate;
    private OrganizationRecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
