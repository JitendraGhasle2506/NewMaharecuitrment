package com.maharecruitment.gov.in.master.controller;

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
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/designations")
public class ManpowerDesignationMasterController {

    private final ManpowerDesignationMasterService service;

    public ManpowerDesignationMasterController(ManpowerDesignationMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ManpowerDesignationMasterResponse>> create(
            @Valid @RequestBody ManpowerDesignationMasterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Designation created successfully", service.create(request)));
    }

    @GetMapping("/{designationId}")
    public ResponseEntity<ApiResponse<ManpowerDesignationMasterResponse>> getById(
            @PathVariable Long designationId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation fetched successfully",
                service.getById(designationId, includeInactive)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ManpowerDesignationMasterResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "designationId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designations fetched successfully",
                service.getAll(includeInactive, pageable)));
    }

    @PutMapping("/{designationId}")
    public ResponseEntity<ApiResponse<ManpowerDesignationMasterResponse>> update(
            @PathVariable Long designationId,
            @Valid @RequestBody ManpowerDesignationMasterRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation updated successfully",
                service.update(designationId, request)));
    }

    @DeleteMapping("/{designationId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long designationId) {
        service.softDelete(designationId);
        return ResponseEntity.ok(ApiResponse.of("Designation deleted successfully", null));
    }

    @PatchMapping("/{designationId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long designationId) {
        service.restore(designationId);
        return ResponseEntity.ok(ApiResponse.of("Designation restored successfully", null));
    }
}
