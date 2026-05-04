package com.maharecruitment.gov.in.attendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<InternalAttendanceSyncResult> runNow() {
        InternalAttendanceSyncResult result = internalAttendanceSyncService.syncCurrentMonthAttendance();
        return ResponseEntity.ok(result);
    }
}
