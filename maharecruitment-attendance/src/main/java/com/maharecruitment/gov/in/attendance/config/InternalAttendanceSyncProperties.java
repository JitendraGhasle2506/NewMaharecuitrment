package com.maharecruitment.gov.in.attendance.config;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "attendance.integration.internal")
public class InternalAttendanceSyncProperties {

    private static final String REPORT_API_PATH = "/third-party/attendance-report-org";
    private static final String UPDATE_API_PATH = "/third-party/update-attendance";

    private boolean enabled = true;

    private String baseUrl = "https://mahaitattendance.espltestingsite.in/api";

    private boolean mobileUpdateEnabled = true;

    private String uniqueCodePrefix = "MahaIT";

    private String schedulerCron = "0 5 11,23 * * *";

    private String schedulerZone = "Asia/Kolkata";

    private boolean currentDateOnly;

    private LocalDate overrideStartDate;

    private LocalDate overrideEndDate;

    private int connectTimeoutSeconds = 5;

    private int readTimeoutSeconds = 15;

    private boolean stopOnUpstreamUnavailable = true;

    private long minRequestIntervalMillis = 1100;

    private int rateLimitRetryAttempts = 2;

    private int rateLimitRetryDelaySeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isMobileUpdateEnabled() {
        return mobileUpdateEnabled;
    }

    public void setMobileUpdateEnabled(boolean mobileUpdateEnabled) {
        this.mobileUpdateEnabled = mobileUpdateEnabled;
    }

    public String reportApiUrl() {
        return endpointUrl(REPORT_API_PATH);
    }

    public String updateApiUrl() {
        return endpointUrl(UPDATE_API_PATH);
    }

    public String getUniqueCodePrefix() {
        return uniqueCodePrefix;
    }

    public void setUniqueCodePrefix(String uniqueCodePrefix) {
        this.uniqueCodePrefix = uniqueCodePrefix;
    }

    public String getSchedulerCron() {
        return schedulerCron;
    }

    public void setSchedulerCron(String schedulerCron) {
        this.schedulerCron = schedulerCron;
    }

    public String getSchedulerZone() {
        return schedulerZone;
    }

    public void setSchedulerZone(String schedulerZone) {
        this.schedulerZone = schedulerZone;
    }

    public boolean isCurrentDateOnly() {
        return currentDateOnly;
    }

    public void setCurrentDateOnly(boolean currentDateOnly) {
        this.currentDateOnly = currentDateOnly;
    }

    public LocalDate getOverrideStartDate() {
        return overrideStartDate;
    }

    public void setOverrideStartDate(LocalDate overrideStartDate) {
        this.overrideStartDate = overrideStartDate;
    }

    public LocalDate getOverrideEndDate() {
        return overrideEndDate;
    }

    public void setOverrideEndDate(LocalDate overrideEndDate) {
        this.overrideEndDate = overrideEndDate;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public boolean isStopOnUpstreamUnavailable() {
        return stopOnUpstreamUnavailable;
    }

    public void setStopOnUpstreamUnavailable(boolean stopOnUpstreamUnavailable) {
        this.stopOnUpstreamUnavailable = stopOnUpstreamUnavailable;
    }

    public long getMinRequestIntervalMillis() {
        return minRequestIntervalMillis;
    }

    public void setMinRequestIntervalMillis(long minRequestIntervalMillis) {
        this.minRequestIntervalMillis = minRequestIntervalMillis;
    }

    public int getRateLimitRetryAttempts() {
        return rateLimitRetryAttempts;
    }

    public void setRateLimitRetryAttempts(int rateLimitRetryAttempts) {
        this.rateLimitRetryAttempts = rateLimitRetryAttempts;
    }

    public int getRateLimitRetryDelaySeconds() {
        return rateLimitRetryDelaySeconds;
    }

    public void setRateLimitRetryDelaySeconds(int rateLimitRetryDelaySeconds) {
        this.rateLimitRetryDelaySeconds = rateLimitRetryDelaySeconds;
    }

    private String endpointUrl(String endpointPath) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Internal attendance API base URL is not configured.");
        }

        String normalizedBaseUrl = baseUrl.trim();
        int endIndex = normalizedBaseUrl.length();
        while (endIndex > 0 && normalizedBaseUrl.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return normalizedBaseUrl.substring(0, endIndex) + endpointPath;
    }
}
