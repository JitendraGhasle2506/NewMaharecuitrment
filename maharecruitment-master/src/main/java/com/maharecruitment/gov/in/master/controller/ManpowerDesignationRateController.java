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
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateRequest;
import com.maharecruitment.gov.in.master.dto.ManpowerDesignationRateResponse;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/designation-rates")
public class ManpowerDesignationRateController {

    private final ManpowerDesignationRateService service;

    public ManpowerDesignationRateController(ManpowerDesignationRateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ManpowerDesignationRateResponse>> create(
            @Valid @RequestBody ManpowerDesignationRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Designation rate created successfully", service.create(request)));
    }

    @GetMapping("/{rateId}")
    public ResponseEntity<ApiResponse<ManpowerDesignationRateResponse>> getById(
            @PathVariable Long rateId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation rate fetched successfully",
                service.getById(rateId, includeInactive)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ManpowerDesignationRateResponse>>> getAll(
            @RequestParam(required = false) Long designationId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "rateId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation rates fetched successfully",
                service.getAll(designationId, includeInactive, pageable)));
    }

    @PutMapping("/{rateId}")
    public ResponseEntity<ApiResponse<ManpowerDesignationRateResponse>> update(
            @PathVariable Long rateId,
            @Valid @RequestBody ManpowerDesignationRateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Designation rate updated successfully",
                service.update(rateId, request)));
    }

    @DeleteMapping("/{rateId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long rateId) {
        service.softDelete(rateId);
        return ResponseEntity.ok(ApiResponse.of("Designation rate deleted successfully", null));
    }

    @PatchMapping("/{rateId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long rateId) {
        service.restore(rateId);
        return ResponseEntity.ok(ApiResponse.of("Designation rate restored successfully", null));
    }
}

