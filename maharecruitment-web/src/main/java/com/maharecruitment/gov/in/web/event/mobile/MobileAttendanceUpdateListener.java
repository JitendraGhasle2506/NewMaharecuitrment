package com.maharecruitment.gov.in.web.event.mobile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceUpdateClient;

@Component
public class MobileAttendanceUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(MobileAttendanceUpdateListener.class);

    private final InternalAttendanceUpdateClient attendanceUpdateClient;

    public MobileAttendanceUpdateListener(InternalAttendanceUpdateClient attendanceUpdateClient) {
        this.attendanceUpdateClient = attendanceUpdateClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckInRecorded(MobileCheckInRecordedEvent event) {
        try {
            attendanceUpdateClient.updateCheckIn(
                    event.employeeCode(),
                    event.attendanceDate(),
                    event.checkInTime());
        } catch (RuntimeException ex) {
            log.error(
                    "Post-commit check-in update failed. employeeCode={}, attendanceDate={}",
                    event.employeeCode(),
                    event.attendanceDate(),
                    ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckOutRecorded(MobileCheckOutRecordedEvent event) {
        try {
            attendanceUpdateClient.updateCheckOut(
                    event.employeeCode(),
                    event.attendanceDate(),
                    event.checkOutTime());
        } catch (RuntimeException ex) {
            log.error(
                    "Post-commit check-out update failed. employeeCode={}, attendanceDate={}",
                    event.employeeCode(),
                    event.attendanceDate(),
                    ex);
        }
    }
}
