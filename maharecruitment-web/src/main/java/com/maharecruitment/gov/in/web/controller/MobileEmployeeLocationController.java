package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeLocationResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeLocationService;

import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/mobile/employee-locations")
public class MobileEmployeeLocationController {

    private final MobileEmployeeLocationService mobileEmployeeLocationService;

    public MobileEmployeeLocationController(MobileEmployeeLocationService mobileEmployeeLocationService) {
        this.mobileEmployeeLocationService = mobileEmployeeLocationService;
    }

    @GetMapping
    public ResponseEntity<MobileEmployeeLocationResponse> getMappedLocations(
            @RequestParam("employeeId") @NotNull Long employeeId) {
        return ResponseEntity.ok(mobileEmployeeLocationService.getMappedLocations(employeeId));
    }
}
