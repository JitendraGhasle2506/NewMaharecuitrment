package com.maharecruitment.gov.in.attendance.client.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
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
    private static final DateTimeFormatter LEGACY_RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final RestClient restClient;
    private final InternalAttendanceSyncProperties properties;
    private final ObjectMapper objectMapper;

    public MahaitInternalAttendanceReportClient(
            InternalAttendanceSyncProperties properties) {
        this.properties = properties;
        this.restClient = createRestClient(properties);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<InternalAttendanceDayRecord> fetchAttendanceReport(LocalDate startDate, LocalDate endDate) {
        validateRequest(startDate, endDate);

        try {
            return fetchAttendanceReportWithRateLimitRetries(startDate, endDate);
        } catch (InternalAttendanceReportClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new InternalAttendanceReportClientUnavailableException(
                    "Internal attendance API is unreachable for organization code "
                            + properties.getOrganizationCode()
                            + " at "
                            + properties.getApiUrl()
                            + ". "
                            + resolveRootCauseMessage(ex),
                    ex);
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Attendance API HTTP error. method=POST, organizationCode={}, startDate={}, endDate={}, status={}, responseHeaders={}, responseBody={}",
                    properties.getOrganizationCode(),
                    REQUEST_DATE_FORMAT.format(startDate),
                    REQUEST_DATE_FORMAT.format(endDate),
                    ex.getStatusCode().value(),
                    sanitizeHeaders(ex.getResponseHeaders()),
                    ex.getResponseBodyAsString());
            throw new InternalAttendanceReportClientException(
                    buildHttpFailureMessage(ex),
                    ex);
        } catch (Exception ex) {
            throw new InternalAttendanceReportClientException(
                    "Failed to fetch internal attendance from API for organization code "
                            + properties.getOrganizationCode()
                            + ".",
                    ex);
        }
    }

    private List<InternalAttendanceDayRecord> fetchAttendanceReportWithRateLimitRetries(
            LocalDate startDate,
            LocalDate endDate) {
        int maxAttempts = Math.max(properties.getRateLimitRetryAttempts(), 0) + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return fetchAttendanceReportOnce(startDate, endDate);
            } catch (RestClientResponseException ex) {
                if (!isTooManyRequests(ex) || attempt >= maxAttempts) {
                    throw ex;
                }

                Duration retryDelay = resolveRateLimitRetryDelay(ex);
                log.warn(
                        "Attendance API rate limit hit for organizationCode={}. retryAttempt={}/{}, retryDelayMs={}",
                        properties.getOrganizationCode(),
                        attempt,
                        maxAttempts - 1,
                        retryDelay.toMillis());
                sleepBeforeRetry(retryDelay);
            }
        }

        throw new InternalAttendanceReportClientException(
                "Attendance API retry loop ended unexpectedly for organization code "
                        + properties.getOrganizationCode()
                        + ".");
    }

    private List<InternalAttendanceDayRecord> fetchAttendanceReportOnce(
            LocalDate startDate,
            LocalDate endDate) {
        String requestUrl = buildRequestUrl(startDate, endDate);
        long startedAtNanos = System.nanoTime();
        log.info(
                "Calling attendance API. method=POST, url={}, organizationCode={}, startDate={}, endDate={}, requestHeaders={}",
                requestUrl,
                properties.getOrganizationCode(),
                REQUEST_DATE_FORMAT.format(startDate),
                REQUEST_DATE_FORMAT.format(endDate),
                "{Accept=[application/json]}");

        ResponseEntity<String> responseEntity = restClient.post()
                .uri(requestUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();

        log.info(
                "Attendance API response received. method=POST, organizationCode={}, status={}, responseHeaders={}, responseBody={}, elapsedMs={}",
                properties.getOrganizationCode(),
                responseEntity.getStatusCode().value(),
                sanitizeHeaders(responseEntity.getHeaders()),
                responseEntity.getBody(),
                elapsedMillis);

        InternalAttendanceReportApiResponse response = parseResponse(responseEntity.getBody());

        if (response == null) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API returned an empty response for organization code "
                            + properties.getOrganizationCode()
                            + ".");
        }

        if (!response.isStatus()) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API rejected the request for organization code "
                            + properties.getOrganizationCode()
                            + ": "
                            + safeMessage(response.getMessage()));
        }

        List<InternalAttendanceReportApiRow> rows = extractRows(response.getData());
        List<InternalAttendanceDayRecord> dayRecords = new ArrayList<>();
        for (InternalAttendanceReportApiRow row : rows) {
            InternalAttendanceDayRecord dayRecord = toDayRecord(row);
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

    private void sleepBeforeRetry(Duration retryDelay) {
        long retryDelayMillis = Math.max(retryDelay.toMillis(), 1_000L);
        try {
            TimeUnit.MILLISECONDS.sleep(retryDelayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalAttendanceReportClientUnavailableException(
                    "Attendance API retry was interrupted for organization code "
                            + properties.getOrganizationCode()
                            + ".",
                    ex);
        }
    }

    private String buildRequestUrl(LocalDate startDate, LocalDate endDate) {
        return UriComponentsBuilder.fromHttpUrl(properties.getApiUrl())
                .queryParam("organization_code", properties.getOrganizationCode())
                .queryParam("start_date", REQUEST_DATE_FORMAT.format(startDate))
                .queryParam("end_date", REQUEST_DATE_FORMAT.format(endDate))
                .toUriString();
    }

    private InternalAttendanceReportApiResponse parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, InternalAttendanceReportApiResponse.class);
        } catch (Exception ex) {
            throw new InternalAttendanceReportClientException(
                    "Attendance API returned a response that could not be parsed as JSON.",
                    ex);
        }
    }

    private List<InternalAttendanceReportApiRow> extractRows(JsonNode data) {
        if (data == null || data.isNull()) {
            return List.of();
        }
        if (data.isArray()) {
            return objectMapper.convertValue(data, new TypeReference<List<InternalAttendanceReportApiRow>>() {
            });
        }
        if (!data.isObject()) {
            return List.of();
        }

        List<InternalAttendanceReportApiRow> rows = new ArrayList<>();
        data.fields().forEachRemaining(entry -> appendRows(entry, rows));
        return rows;
    }

    private void appendRows(Map.Entry<String, JsonNode> entry, List<InternalAttendanceReportApiRow> rows) {
        String employeeCode = entry.getKey();
        JsonNode value = entry.getValue();
        if (value == null || value.isNull()) {
            return;
        }

        List<InternalAttendanceReportApiRow> employeeRows;
        if (value.isArray()) {
            employeeRows = objectMapper.convertValue(value, new TypeReference<List<InternalAttendanceReportApiRow>>() {
            });
        } else if (value.isObject()) {
            employeeRows = List.of(objectMapper.convertValue(value, InternalAttendanceReportApiRow.class));
        } else {
            return;
        }

        for (InternalAttendanceReportApiRow row : employeeRows) {
            if (row != null && !StringUtils.hasText(row.getCode())) {
                row.setCode(employeeCode);
            }
            rows.add(row);
        }
    }

    private InternalAttendanceDayRecord toDayRecord(InternalAttendanceReportApiRow row) {
        if (row == null || !StringUtils.hasText(row.getDate())) {
            log.warn("Skipping attendance API row without a valid date.");
            return null;
        }

        try {
            return new InternalAttendanceDayRecord(
                    normalizeText(row.getName()),
                    normalizeText(row.getCode()),
                    parseResponseDate(row.getDate()),
                    normalizeText(row.getInTime()),
                    normalizeText(row.getOutTime()),
                    normalizeText(row.getStatus()));
        } catch (DateTimeParseException ex) {
            log.warn("Skipping attendance API row with invalid date. employeeCode={}, rawDate={}",
                    row.getCode(),
                    row.getDate());
            return null;
        }
    }

    private LocalDate parseResponseDate(String rawDate) {
        String value = rawDate.trim();
        try {
            return LocalDate.parse(value, RESPONSE_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value, LEGACY_RESPONSE_DATE_FORMAT);
        }
    }

    private void validateRequest(LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(properties.getOrganizationCode())) {
            throw new IllegalArgumentException("Organization code is required to fetch attendance data.");
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

    private String buildHttpFailureMessage(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String statusText = safeMessage(ex.getStatusText());
        String guidance = switch (status) {
            case 400 -> "Bad request. Verify organization_code, start_date, and end_date query parameters.";
            case 401 -> "Unauthorized. Verify the attendance API authentication configuration.";
            case 403 -> "Forbidden. The configured credentials are not allowed to access attendance data.";
            case 404 -> "Endpoint not found. Verify the attendance API URL.";
            case 405 -> "Method Not Allowed. The attendance API currently allows HTTP POST for this route.";
            case 408 -> "Request timeout from upstream attendance API.";
            case 429 -> "Rate limited by upstream attendance API.";
            case 500 -> "Internal server error from upstream attendance API.";
            case 502 -> "Bad gateway from upstream attendance API.";
            case 503 -> "Attendance API is temporarily unavailable.";
            default -> "Unexpected upstream attendance API error.";
        };
        return "Attendance API returned HTTP "
                + status
                + " for organization code "
                + properties.getOrganizationCode()
                + ". "
                + guidance
                + " "
                + statusText;
    }

    private Map<String, List<String>> sanitizeHeaders(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) -> {
            if (isSecretHeader(name)) {
                sanitized.put(name, List.of("[REDACTED]"));
            } else {
                sanitized.put(name, values);
            }
        });
        return sanitized;
    }

    private boolean isSecretHeader(String name) {
        return StringUtils.hasText(name)
                && (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                        || HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(name)
                        || name.toLowerCase().contains("api-key")
                        || name.toLowerCase().contains("token")
                        || name.toLowerCase().contains("secret"));
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
