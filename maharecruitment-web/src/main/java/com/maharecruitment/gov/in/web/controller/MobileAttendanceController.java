package com.maharecruitment.gov.in.web.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceAction;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceHistoryResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceJsonRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceMarkJsonRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceException;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceService;
import com.maharecruitment.gov.in.web.service.mobile.MobileAttendanceJsonImageMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/mobile/attendance")
public class MobileAttendanceController {

    private final MobileAttendanceService mobileAttendanceService;
    private final MobileAttendanceJsonImageMapper mobileAttendanceJsonImageMapper;

    public MobileAttendanceController(
            MobileAttendanceService mobileAttendanceService,
            MobileAttendanceJsonImageMapper mobileAttendanceJsonImageMapper) {
        this.mobileAttendanceService = mobileAttendanceService;
        this.mobileAttendanceJsonImageMapper = mobileAttendanceJsonImageMapper;
    }

    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobileAttendanceResponse> checkIn(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam("latitude") @NotNull BigDecimal latitude,
            @RequestParam("longitude") @NotNull BigDecimal longitude,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(mobileAttendanceService.checkIn(
                employeeId,
                latitude,
                longitude,
                locationAddress,
                image));
    }

    @PostMapping(value = "/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileAttendanceResponse> checkInJson(
            @Valid @RequestBody MobileAttendanceJsonRequest request) {
        return ResponseEntity.ok(mobileAttendanceService.checkIn(
                request.employeeId(),
                request.latitude(),
                request.longitude(),
                request.locationAddress(),
                mobileAttendanceJsonImageMapper.toMultipartFile(request)));
    }

    @PostMapping(value = "/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobileAttendanceResponse> checkOut(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam("latitude") @NotNull BigDecimal latitude,
            @RequestParam("longitude") @NotNull BigDecimal longitude,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(mobileAttendanceService.checkOut(
                employeeId,
                latitude,
                longitude,
                locationAddress,
                image));
    }

    @PostMapping(value = "/check-out", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileAttendanceResponse> checkOutJson(
            @Valid @RequestBody MobileAttendanceJsonRequest request) {
        return ResponseEntity.ok(mobileAttendanceService.checkOut(
                request.employeeId(),
                request.latitude(),
                request.longitude(),
                request.locationAddress(),
                mobileAttendanceJsonImageMapper.toMultipartFile(request)));
    }

    @PostMapping(value = "/mark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobileAttendanceResponse> mark(
            @RequestParam(value = "attendanceFlag", required = false) String attendanceFlag,
            @RequestParam(value = "flag", required = false) String flag,
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam("latitude") @NotNull BigDecimal latitude,
            @RequestParam("longitude") @NotNull BigDecimal longitude,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(markAttendance(
                resolveAttendanceAction(attendanceFlag, flag),
                employeeId,
                latitude,
                longitude,
                locationAddress,
                image));
    }

    @PostMapping(value = "/mark", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MobileAttendanceResponse> markJson(
            @Valid @RequestBody MobileAttendanceMarkJsonRequest request) {
        return ResponseEntity.ok(markAttendance(
                request.attendanceFlag(),
                request.employeeId(),
                request.latitude(),
                request.longitude(),
                request.locationAddress(),
                mobileAttendanceJsonImageMapper.toMultipartFile(request)));
    }

    @GetMapping("/history")
    public ResponseEntity<MobileAttendanceHistoryResponse> history(
            @RequestParam("employeeId") @NotNull Long employeeId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate) {
        LocalDate selectedFromDate = fromDate != null ? fromDate : date;
        LocalDate selectedToDate = toDate != null ? toDate : selectedFromDate;
        return ResponseEntity.ok(mobileAttendanceService.getHistory(employeeId, selectedFromDate, selectedToDate));
    }

    private MobileAttendanceResponse markAttendance(
            MobileAttendanceAction attendanceAction,
            Long employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationAddress,
            MultipartFile image) {
        if (attendanceAction == MobileAttendanceAction.CHECK_IN) {
            return mobileAttendanceService.checkIn(employeeId, latitude, longitude, locationAddress, image);
        }
        if (attendanceAction == MobileAttendanceAction.CHECK_OUT) {
            return mobileAttendanceService.checkOut(employeeId, latitude, longitude, locationAddress, image);
        }
        throw new MobileAttendanceException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "ATTENDANCE_FLAG_REQUIRED",
                "Attendance flag must be CHECK_IN or CHECK_OUT.");
    }

    private MobileAttendanceAction resolveAttendanceAction(String attendanceFlag, String flag) {
        String selectedFlag = org.springframework.util.StringUtils.hasText(attendanceFlag) ? attendanceFlag : flag;
        try {
            return MobileAttendanceAction.from(selectedFlag);
        } catch (IllegalArgumentException ex) {
            throw new MobileAttendanceException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_ATTENDANCE_FLAG",
                    "Attendance flag must be CHECK_IN or CHECK_OUT.");
        }
    }
}
