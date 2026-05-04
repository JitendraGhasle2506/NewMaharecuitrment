package com.maharecruitment.gov.in.attendance.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.attendance.service.InternalAttendanceSyncService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;

@Component
@ConditionalOnProperty(prefix = "attendance.integration.internal", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InternalAttendanceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(InternalAttendanceSyncScheduler.class);

    private final InternalAttendanceSyncService internalAttendanceSyncService;

    public InternalAttendanceSyncScheduler(InternalAttendanceSyncService internalAttendanceSyncService) {
        this.internalAttendanceSyncService = internalAttendanceSyncService;
    }

    @Scheduled(
            cron = "${attendance.integration.internal.scheduler-cron:0 5 11,23 * * *}",
            zone = "${attendance.integration.internal.scheduler-zone:Asia/Kolkata}")
    public void syncCurrentMonthAttendance() {
        try {
            InternalAttendanceSyncResult result = internalAttendanceSyncService.syncCurrentMonthAttendance();
            log.info(
                    "Internal attendance sync completed. startDate={}, endDate={}, attempted={}, synced={}, skipped={}, failed={}, upsertedRows={}",
                    result.getStartDate(),
                    result.getEndDate(),
                    result.getEmployeesAttempted(),
                    result.getEmployeesSynced(),
                    result.getEmployeesSkipped(),
                    result.getEmployeesFailed(),
                    result.getAttendanceRowsUpserted());
        } catch (Exception ex) {
            log.error("Internal attendance sync scheduler failed.", ex);
        }
    }
}
