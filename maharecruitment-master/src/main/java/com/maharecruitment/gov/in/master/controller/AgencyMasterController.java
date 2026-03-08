package com.maharecruitment.gov.in.master.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.dto.AgencyStatusUpdateRequest;
import com.maharecruitment.gov.in.master.dto.ApiResponse;
import com.maharecruitment.gov.in.master.service.AgencyMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/agencies")
public class AgencyMasterController {

    private final AgencyMasterService service;

    public AgencyMasterController(AgencyMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgencyMasterResponse>> create(
            @Valid @RequestBody AgencyMasterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Agency created successfully", service.create(request)));
    }

    @GetMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyMasterResponse>> getById(@PathVariable Long agencyId) {
        return ResponseEntity.ok(ApiResponse.of("Agency fetched successfully", service.getById(agencyId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AgencyMasterResponse>>> getAll(
            @PageableDefault(size = 20, sort = "agencyId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of("Agencies fetched successfully", service.getAll(pageable)));
    }

    @PutMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyMasterResponse>> update(
            @PathVariable Long agencyId,
            @Valid @RequestBody AgencyMasterRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Agency updated successfully", service.update(agencyId, request)));
    }

    @PutMapping("/{agencyId}/status")
    public ResponseEntity<ApiResponse<AgencyMasterResponse>> updateStatus(
            @PathVariable Long agencyId,
            @Valid @RequestBody AgencyStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Agency status updated successfully",
                service.updateStatus(agencyId, request.getStatus())));
    }

    @DeleteMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyMasterResponse>> delete(@PathVariable Long agencyId) {
        return ResponseEntity.ok(ApiResponse.of("Agency deleted successfully", service.delete(agencyId)));
    }
}
