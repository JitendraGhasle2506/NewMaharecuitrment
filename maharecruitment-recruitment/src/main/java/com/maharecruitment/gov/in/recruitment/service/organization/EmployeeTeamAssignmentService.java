package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamAssignmentResponse;

public interface EmployeeTeamAssignmentService {

    List<EmployeeTeamAssignmentResponse> getAssignments();

    EmployeeTeamAssignmentResponse assignTeam(Long employeeId, Long teamId);

    void clearTeam(Long employeeId);
}
