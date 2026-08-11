package com.maharecruitment.gov.in.web.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.mobile.MobileCompOffValidationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplicationResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApplyRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveApprovalsResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveDecisionRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveEmployeeRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLeaveOptionsResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileLeaveService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/mobile/leaves")
public class MobileLeaveController {

    private final MobileLeaveService mobileLeaveService;

    public MobileLeaveController(MobileLeaveService mobileLeaveService) {
        this.mobileLeaveService = mobileLeaveService;
    }

    @GetMapping("/options")
    public ResponseEntity<MobileLeaveOptionsResponse> getOptions(
            @RequestParam("employeeId") @NotNull Long employeeId) {
        return ResponseEntity.ok(mobileLeaveService.getOptions(employeeId));
    }

    @PostMapping
    public ResponseEntity<MobileLeaveApplicationResponse> apply(
            @Valid @RequestBody MobileLeaveApplyRequest request) {
        MobileLeaveApplicationResponse response = mobileLeaveService.apply(request);
        return ResponseEntity
                .created(URI.create("/api/mobile/leaves/" + response.leaveApplication().leaveId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<MobileLeaveHistoryResponse> getApplications(
            @RequestParam("employeeId") @NotNull Long employeeId) {
        return ResponseEntity.ok(mobileLeaveService.getApplications(employeeId));
    }

    @GetMapping("/comp-off/validate")
    public ResponseEntity<MobileCompOffValidationResponse> validateCompOffWorkedDate(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam("workedDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @NotNull LocalDate workedDate) {
        return ResponseEntity.ok(mobileLeaveService.validateCompOffWorkedDate(employeeId, workedDate));
    }

    @PostMapping("/{leaveId}/cancel")
    public ResponseEntity<MobileLeaveApplicationResponse> cancel(
            @PathVariable("leaveId") @Positive Long leaveId,
            @Valid @RequestBody MobileLeaveEmployeeRequest request) {
        return ResponseEntity.ok(mobileLeaveService.cancel(request.employeeId(), leaveId));
    }

    @GetMapping("/approvals")
    public ResponseEntity<MobileLeaveApprovalsResponse> getApprovals(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(mobileLeaveService.getApprovals(employeeId, query));
    }

    @PostMapping("/approvals/{leaveId}/approve")
    public ResponseEntity<MobileLeaveApplicationResponse> approve(
            @PathVariable("leaveId") @Positive Long leaveId,
            @Valid @RequestBody MobileLeaveDecisionRequest request) {
        return ResponseEntity.ok(mobileLeaveService.approve(request.employeeId(), leaveId, request.remarks()));
    }

    @PostMapping("/approvals/{leaveId}/reject")
    public ResponseEntity<MobileLeaveApplicationResponse> reject(
            @PathVariable("leaveId") @Positive Long leaveId,
            @Valid @RequestBody MobileLeaveDecisionRequest request) {
        return ResponseEntity.ok(mobileLeaveService.reject(request.employeeId(), leaveId, request.remarks()));
    }
}
