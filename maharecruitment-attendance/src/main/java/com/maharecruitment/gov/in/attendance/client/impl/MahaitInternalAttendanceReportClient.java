package com.maharecruitment.gov.in.attendance.client.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClient;
import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClientException;
import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClientUnavailableException;
import com.maharecruitment.gov.in.attendance.client.dto.InternalAttendanceReportApiResponse;
import com.maharecruitment.gov.in.attendance.client.dto.InternalAttendanceReportApiRow;
import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

@Component
public class MahaitInternalAttendanceReportClient implements InternalAttendanceReportClient {

    private static final Logger log = LoggerFactory.getLogger(MahaitInternalAttendanceReportClient.class);
    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter RESPONSE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final InternalAttendanceSyncProperties properties;

    public MahaitInternalAttendanceReportClient(
            InternalAttendanceSyncProperties properties) {
        this.properties = properties;
        this.restClient = createRestClient(properties);
    }

    @Override
    public List<InternalAttendanceDayRecord> fetchAttendanceReport(
            String uniqueCode,
            LocalDate startDate,
            LocalDate endDate) {
        validateRequest(uniqueCode, startDate, endDate);

        try {
            return fetchAttendanceReportWithRateLimitRetries(uniqueCode, startDate, endDate);
        } catch (InternalAttendanceReportClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new InternalAttendanceReportClientUnavailableException(
                    "Internal attendance API is unreachable for unique code "
                            + uniqueCode
                            + " at "
                            + properties.getApiUrl()
                            + ". "
                            + resolveRootCauseMessage(ex),
                    ex);
        } catch (RestClientResponseException ex) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API returned HTTP "
                            + ex.getStatusCode().value()
                            + " for unique code "
                            + uniqueCode
                            + ". "
                            + safeMessage(ex.getStatusText()),
                    ex);
        } catch (Exception ex) {
            throw new InternalAttendanceReportClientException(
                    "Failed to fetch internal attendance from API for unique code " + uniqueCode + ".",
                    ex);
        }
    }

    private List<InternalAttendanceDayRecord> fetchAttendanceReportWithRateLimitRetries(
            String uniqueCode,
            LocalDate startDate,
            LocalDate endDate) {
        int maxAttempts = Math.max(properties.getRateLimitRetryAttempts(), 0) + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return fetchAttendanceReportOnce(uniqueCode, startDate, endDate);
            } catch (RestClientResponseException ex) {
                if (!isTooManyRequests(ex) || attempt >= maxAttempts) {
                    throw ex;
                }

                Duration retryDelay = resolveRateLimitRetryDelay(ex);
                log.warn(
                        "Attendance API rate limit hit for uniqueCode={}. retryAttempt={}/{}, retryDelayMs={}",
                        uniqueCode,
                        attempt,
                        maxAttempts - 1,
                        retryDelay.toMillis());
                sleepBeforeRetry(uniqueCode, retryDelay);
            }
        }

        throw new InternalAttendanceReportClientException(
                "Attendance API retry loop ended unexpectedly for unique code " + uniqueCode + ".");
    }

    private List<InternalAttendanceDayRecord> fetchAttendanceReportOnce(
            String uniqueCode,
            LocalDate startDate,
            LocalDate endDate) {
        InternalAttendanceReportApiResponse response = restClient.post()
                .uri(properties.getApiUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(buildPayload(uniqueCode, startDate, endDate))
                .retrieve()
                .body(InternalAttendanceReportApiResponse.class);

        if (response == null) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API returned an empty response for unique code " + uniqueCode + ".");
        }

        if (!response.isStatus()) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API rejected the request for unique code " + uniqueCode + ": "
                            + safeMessage(response.getMessage()));
        }

        List<InternalAttendanceReportApiRow> rows = response.getData() == null ? List.of() : response.getData();
        List<InternalAttendanceDayRecord> dayRecords = new ArrayList<>();
        for (InternalAttendanceReportApiRow row : rows) {
            InternalAttendanceDayRecord dayRecord = toDayRecord(uniqueCode, row);
            if (dayRecord != null) {
                dayRecords.add(dayRecord);
            }
        }

        return dayRecords.stream()
                .sorted(Comparator.comparing(InternalAttendanceDayRecord::getAttendanceDate))
                .toList();
    }

    private RestClient createRestClient(InternalAttendanceSyncProperties syncProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(syncProperties.getConnectTimeoutSeconds(), 1) * 1000);
        requestFactory.setReadTimeout(Math.max(syncProperties.getReadTimeoutSeconds(), 1) * 1000);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private boolean isTooManyRequests(RestClientResponseException ex) {
        return ex.getStatusCode().value() == 429;
    }

    private Duration resolveRateLimitRetryDelay(RestClientResponseException ex) {
        HttpHeaders responseHeaders = ex.getResponseHeaders();
        String retryAfter = responseHeaders != null ? responseHeaders.getFirst(HttpHeaders.RETRY_AFTER) : null;
        if (StringUtils.hasText(retryAfter)) {
            try {
                long retryAfterSeconds = Long.parseLong(retryAfter.trim());
                if (retryAfterSeconds > 0L) {
                    return Duration.ofSeconds(retryAfterSeconds);
                }
            } catch (NumberFormatException ignored) {
                log.debug("Ignoring non-numeric Retry-After header from attendance API: {}", retryAfter);
            }
        }

        return Duration.ofSeconds(Math.max(properties.getRateLimitRetryDelaySeconds(), 1));
    }

    private void sleepBeforeRetry(String uniqueCode, Duration retryDelay) {
        long retryDelayMillis = Math.max(retryDelay.toMillis(), 1_000L);
        try {
            TimeUnit.MILLISECONDS.sleep(retryDelayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalAttendanceReportClientUnavailableException(
                    "Attendance API retry was interrupted for unique code " + uniqueCode + ".",
                    ex);
        }
    }

    private MultiValueMap<String, String> buildPayload(String uniqueCode, LocalDate startDate, LocalDate endDate) {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("employee_code", uniqueCode);
        payload.add("start_date", REQUEST_DATE_FORMAT.format(startDate));
        payload.add("end_date", REQUEST_DATE_FORMAT.format(endDate));
        return payload;
    }

    private InternalAttendanceDayRecord toDayRecord(String requestedUniqueCode, InternalAttendanceReportApiRow row) {
        if (row == null || !StringUtils.hasText(row.getDate())) {
            log.warn("Skipping attendance API row without a valid date. uniqueCode={}", requestedUniqueCode);
            return null;
        }

        try {
            return new InternalAttendanceDayRecord(
                    normalizeText(row.getName()),
                    normalizeText(row.getCode()),
                    LocalDate.parse(row.getDate().trim(), RESPONSE_DATE_FORMAT),
                    normalizeText(row.getInTime()),
                    normalizeText(row.getOutTime()),
                    normalizeText(row.getStatus()));
        } catch (DateTimeParseException ex) {
            log.warn("Skipping attendance API row with invalid date. uniqueCode={}, rawDate={}",
                    requestedUniqueCode,
                    row.getDate());
            return null;
        }
    }

    private void validateRequest(String uniqueCode, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(uniqueCode)) {
            throw new IllegalArgumentException("Unique code is required to fetch attendance data.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required to fetch attendance data.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date for attendance sync.");
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safeMessage(String value) {
        return StringUtils.hasText(value) ? value.trim() : "Unknown upstream error.";
    }

    private String resolveRootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable deepest = throwable;
        while (current != null) {
            deepest = current;
            current = current.getCause();
        }

        String message = deepest != null ? deepest.getMessage() : null;
        if (StringUtils.hasText(message)) {
            return message.trim();
        }
        return deepest != null ? deepest.getClass().getSimpleName() + "." : "Unknown connectivity failure.";
    }
}
