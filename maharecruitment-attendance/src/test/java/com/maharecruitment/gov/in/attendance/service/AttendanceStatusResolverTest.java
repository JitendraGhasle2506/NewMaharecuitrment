package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;

class AttendanceStatusResolverTest {

    @Test
    void resolveDisplayStatusTreatsWeekOffWithCompletePunchAsPresent() {
        assertEquals("PRESENT", AttendanceStatusResolver.resolveDisplayStatus("WO", "09:00", "18:00"));
        assertEquals("P", AttendanceStatusResolver.resolveStatusCode("WEEK_OFF", "09:00", "18:00"));
    }

    @Test
    void resolveDisplayStatusKeepsWeekOffWhenPunchIsIncomplete() {
        assertEquals("WEEK_OFF", AttendanceStatusResolver.resolveDisplayStatus("WO", "09:00", null));
        assertEquals("W", AttendanceStatusResolver.resolveStatusCode("WO", "09:00", null));
    }

    @Test
    void resolveDisplayStatusFallsBackToPunchTimesWhenStatusIsBlank() {
        assertEquals("PRESENT", AttendanceStatusResolver.resolveDisplayStatus(null, "09:00", null));
        assertNull(AttendanceStatusResolver.resolveDisplayStatus(null, null, null));
    }

    @Test
    void resolveDisplayStatusTreatsMobileEventAsPresentWhenApiStatusIsAbsent() {
        DailyAttendanceInternalEntity attendance = new DailyAttendanceInternalEntity();
        attendance.setStatus("ABSENT");
        attendance.setCheckInTime(LocalTime.of(9, 52));

        assertEquals("PRESENT", AttendanceStatusResolver.resolveDisplayStatus(attendance));
        assertEquals("P", AttendanceStatusResolver.resolveStatusCode(attendance));
    }
}
