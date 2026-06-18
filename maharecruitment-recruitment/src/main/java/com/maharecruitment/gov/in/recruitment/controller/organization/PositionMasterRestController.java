package com.maharecruitment.gov.in.recruitment.controller.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionResponse;
import com.maharecruitment.gov.in.recruitment.service.organization.OrganizationManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/positions")
public class PositionMasterRestController {

    private final OrganizationManagementService organizationManagementService;

    public PositionMasterRestController(OrganizationManagementService organizationManagementService) {
        this.organizationManagementService = organizationManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PositionResponse>> create(@Valid @RequestBody PositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Position created successfully", organizationManagementService.createPosition(request)));
    }

    @GetMapping("/{positionId}")
    public ResponseEntity<ApiResponse<PositionResponse>> get(@PathVariable Long positionId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Position fetched successfully",
                organizationManagementService.getPosition(positionId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PositionResponse>>> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cellId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Positions fetched successfully",
                organizationManagementService.searchPositions(
                        projectId,
                        cellId,
                        teamId,
                        includeInactive,
                        search,
                        pageable)));
    }

    @PutMapping("/{positionId}")
    public ResponseEntity<ApiResponse<PositionResponse>> update(
            @PathVariable Long positionId,
            @Valid @RequestBody PositionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Position updated successfully",
                organizationManagementService.updatePosition(positionId, request)));
    }

    @DeleteMapping("/{positionId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long positionId) {
        organizationManagementService.changePositionStatus(positionId, false);
        return ResponseEntity.ok(ApiResponse.of("Position deactivated successfully", null));
    }

    @PatchMapping("/{positionId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long positionId) {
        organizationManagementService.changePositionStatus(positionId, true);
        return ResponseEntity.ok(ApiResponse.of("Position restored successfully", null));
    }
}
