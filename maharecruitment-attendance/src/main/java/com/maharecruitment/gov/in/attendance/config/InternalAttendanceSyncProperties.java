package com.maharecruitment.gov.in.attendance.config;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "attendance.integration.internal")
public class InternalAttendanceSyncProperties {

    private boolean enabled = true;

    private String apiUrl = "https://mahait.fitechit.in/api/attendance-report-employee";

    private String uniqueCodePrefix = "MahaIT";

    private String schedulerCron = "0 5 11,23 * * *";

    private String schedulerZone = "Asia/Kolkata";

    private boolean currentDateOnly;

    private LocalDate overrideStartDate;

    private LocalDate overrideEndDate;

    private int connectTimeoutSeconds = 5;

    private int readTimeoutSeconds = 15;

    private boolean stopOnUpstreamUnavailable = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
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
}
