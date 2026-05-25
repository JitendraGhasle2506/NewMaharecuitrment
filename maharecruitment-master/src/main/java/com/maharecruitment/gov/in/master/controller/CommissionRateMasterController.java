package com.maharecruitment.gov.in.master.controller;

import java.time.LocalDate;

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
import com.maharecruitment.gov.in.master.dto.CommissionRateAuditLogResponse;
import com.maharecruitment.gov.in.master.dto.CommissionRateRequest;
import com.maharecruitment.gov.in.master.dto.CommissionRateResponse;
import com.maharecruitment.gov.in.master.entity.CommissionCode;
import com.maharecruitment.gov.in.master.service.CommissionRateMasterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/master/commission-rates")
public class CommissionRateMasterController {

    private final CommissionRateMasterService service;

    public CommissionRateMasterController(CommissionRateMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommissionRateResponse>> create(
            @Valid @RequestBody CommissionRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Commission rate created successfully", service.create(request)));
    }

    @GetMapping("/{commissionRateId}")
    public ResponseEntity<ApiResponse<CommissionRateResponse>> getById(
            @PathVariable Long commissionRateId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.of(
                "Commission rate fetched successfully",
                service.getById(commissionRateId, includeInactive)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommissionRateResponse>>> getAll(
            @RequestParam(required = false) CommissionCode commissionCode,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = "commissionRateId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Commission rates fetched successfully",
                service.getAll(commissionCode, includeInactive, pageable)));
    }

    @GetMapping("/applicable")
    public ResponseEntity<ApiResponse<CommissionRateResponse>> getApplicableRate(
            @RequestParam CommissionCode commissionCode,
            @RequestParam(required = false) LocalDate effectiveDate) {
        return ResponseEntity.ok(ApiResponse.of(
                "Applicable commission rate fetched successfully",
                service.getApplicableRate(commissionCode, effectiveDate)));
    }

    @GetMapping("/{commissionRateId}/logs")
    public ResponseEntity<ApiResponse<Page<CommissionRateAuditLogResponse>>> getAuditLogs(
            @PathVariable Long commissionRateId,
            @PageableDefault(size = 20, sort = "actionTimestamp") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(
                "Commission rate audit logs fetched successfully",
                service.getAuditLogs(commissionRateId, pageable)));
    }

    @PutMapping("/{commissionRateId}")
    public ResponseEntity<ApiResponse<CommissionRateResponse>> update(
            @PathVariable Long commissionRateId,
            @Valid @RequestBody CommissionRateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Commission rate updated successfully",
                service.update(commissionRateId, request)));
    }

    @DeleteMapping("/{commissionRateId}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long commissionRateId) {
        service.softDelete(commissionRateId);
        return ResponseEntity.ok(ApiResponse.of("Commission rate deactivated successfully", null));
    }

    @PatchMapping("/{commissionRateId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long commissionRateId) {
        service.restore(commissionRateId);
        return ResponseEntity.ok(ApiResponse.of("Commission rate restored successfully", null));
    }
}
