package com.maharecruitment.gov.in.attendance.client.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceUpdateClient;
import com.maharecruitment.gov.in.attendance.client.InternalAttendanceUpdateClientException;
import com.maharecruitment.gov.in.attendance.client.dto.InternalAttendanceCheckInUpdateRequest;
import com.maharecruitment.gov.in.attendance.client.dto.InternalAttendanceCheckOutUpdateRequest;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

@Component
public class MahaitInternalAttendanceUpdateClient implements InternalAttendanceUpdateClient {

    private static final Logger log = LoggerFactory.getLogger(MahaitInternalAttendanceUpdateClient.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RestClient restClient;
    private final InternalAttendanceSyncProperties properties;

    @Autowired
    public MahaitInternalAttendanceUpdateClient(InternalAttendanceSyncProperties properties) {
        this(createRestClient(properties), properties);
    }

    MahaitInternalAttendanceUpdateClient(
            RestClient restClient,
            InternalAttendanceSyncProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void updateCheckIn(
            String employeeCode,
            LocalDate attendanceDate,
            LocalTime checkInTime) {
        if (!properties.isMobileUpdateEnabled()) {
            return;
        }

        validate(employeeCode, attendanceDate, checkInTime);
        submit(
                new InternalAttendanceCheckInUpdateRequest(
                        employeeCode.trim(),
                        DATE_FORMAT.format(attendanceDate),
                        TIME_FORMAT.format(checkInTime)),
                "CHECK_IN",
                employeeCode,
                attendanceDate);
    }

    @Override
    public void updateCheckOut(
            String employeeCode,
            LocalDate attendanceDate,
            LocalTime checkOutTime) {
        if (!properties.isMobileUpdateEnabled()) {
            return;
        }

        validate(employeeCode, attendanceDate, checkOutTime);
        submit(
                new InternalAttendanceCheckOutUpdateRequest(
                        employeeCode.trim(),
                        DATE_FORMAT.format(attendanceDate),
                        TIME_FORMAT.format(checkOutTime)),
                "CHECK_OUT",
                employeeCode,
                attendanceDate);
    }

    private void submit(
            Object requestBody,
            String action,
            String employeeCode,
            LocalDate attendanceDate) {
        try {
            restClient.post()
                    .uri(properties.updateApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Attendance update API call completed. action={}, employeeCode={}, attendanceDate={}",
                    action,
                    employeeCode,
                    attendanceDate);
        } catch (RestClientException ex) {
            throw new InternalAttendanceUpdateClientException(
                    "Unable to update " + action + " through the internal attendance API.",
                    ex);
        }
    }

    private void validate(
            String employeeCode,
            LocalDate attendanceDate,
            LocalTime attendanceTime) {
        if (!StringUtils.hasText(employeeCode)) {
            throw new IllegalArgumentException("Employee code is required for attendance update.");
        }
        if (attendanceDate == null) {
            throw new IllegalArgumentException("Attendance date is required for attendance update.");
        }
        if (attendanceTime == null) {
            throw new IllegalArgumentException("Attendance time is required for attendance update.");
        }
    }

    private static RestClient createRestClient(InternalAttendanceSyncProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(properties.getConnectTimeoutSeconds(), 1) * 1000);
        requestFactory.setReadTimeout(Math.max(properties.getReadTimeoutSeconds(), 1) * 1000);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
