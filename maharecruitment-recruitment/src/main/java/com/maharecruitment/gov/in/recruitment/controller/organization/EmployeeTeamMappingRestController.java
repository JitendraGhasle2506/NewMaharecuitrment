package com.maharecruitment.gov.in.recruitment.controller.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingResponse;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/employee-team-mappings")
public class EmployeeTeamMappingRestController {

    private final OrganizationManagementService organizationManagementService;

    public EmployeeTeamMappingRestController(OrganizationManagementService organizationManagementService) {
        this.organizationManagementService = organizationManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeTeamMappingResponse>> create(
            @Valid @RequestBody EmployeeTeamMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        "Employee team mapping created successfully",
                        organizationManagementService.createMapping(request)));
    }

    @GetMapping("/{mappingId}")
    public ResponseEntity<ApiResponse<EmployeeTeamMappingResponse>> get(@PathVariable Long mappingId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee team mapping fetched successfully",
                organizationManagementService.getMapping(mappingId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeTeamMappingResponse>>> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee team mappings fetched successfully",
                organizationManagementService.searchMappings(
                        projectId,
                        cellId,
                        teamId,
                        includeInactive,
                        search,
                        pageable)));
    }

    @PutMapping("/{mappingId}")
    public ResponseEntity<ApiResponse<EmployeeTeamMappingResponse>> update(
            @PathVariable Long mappingId,
            @Valid @RequestBody EmployeeTeamMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Employee team mapping updated successfully",
                organizationManagementService.updateMapping(mappingId, request)));
    }

    @DeleteMapping("/{mappingId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long mappingId) {
        organizationManagementService.deactivateMapping(mappingId);
        return ResponseEntity.ok(ApiResponse.of("Employee team mapping deactivated successfully", null));
    }
}
