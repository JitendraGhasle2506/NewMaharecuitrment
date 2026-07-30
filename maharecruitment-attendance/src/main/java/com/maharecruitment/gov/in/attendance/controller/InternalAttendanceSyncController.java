package com.maharecruitment.gov.in.attendance.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.attendance.service.InternalAttendanceSyncService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;

@RestController
@RequestMapping("/attendance/internal-sync")
public class InternalAttendanceSyncController {

    private final InternalAttendanceSyncService internalAttendanceSyncService;

    public InternalAttendanceSyncController(InternalAttendanceSyncService internalAttendanceSyncService) {
        this.internalAttendanceSyncService = internalAttendanceSyncService;
    }

    @PostMapping("/run-now")
    public ResponseEntity<InternalAttendanceSyncResult> runNow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        InternalAttendanceSyncResult result;
        if (startDate == null && endDate == null) {
            result = internalAttendanceSyncService.syncScheduledAttendance();
        } else if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both startDate and endDate are required when running a manual attendance sync.");
        } else if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date must be on or after the start date.");
        } else {
            result = internalAttendanceSyncService.syncAttendance(startDate, endDate);
        }
        return ResponseEntity.ok(result);
    }
}
