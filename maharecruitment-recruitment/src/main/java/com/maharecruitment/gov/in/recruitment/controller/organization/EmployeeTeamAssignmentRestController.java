package com.maharecruitment.gov.in.recruitment.controller.organization;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamAssignmentRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamAssignmentResponse;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeTeamAssignmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/employee-team-assignments")
public class EmployeeTeamAssignmentRestController {

    private final EmployeeTeamAssignmentService assignmentService;

    public EmployeeTeamAssignmentRestController(EmployeeTeamAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeTeamAssignmentResponse>>> getAssignments() {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee team assignments fetched successfully",
                assignmentService.getAssignments()));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeTeamAssignmentResponse>> assignTeam(
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeTeamAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee team assignment saved successfully",
                assignmentService.assignTeam(employeeId, request.teamId())));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<Void>> clearTeam(@PathVariable Long employeeId) {
        assignmentService.clearTeam(employeeId);
        return ResponseEntity.ok(ApiResponse.of("Employee team assignment removed successfully", null));
    }
}
