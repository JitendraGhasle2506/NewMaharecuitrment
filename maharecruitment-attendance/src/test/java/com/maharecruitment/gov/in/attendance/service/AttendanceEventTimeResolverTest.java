package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.service.AttendanceEventTimeResolver.AttendanceEventWindow;

class AttendanceEventTimeResolverTest {

    @Test
    void resolvesEarliestInAndLatestOutAcrossBiometricAndMobileEvents() {
        DailyAttendanceInternalEntity attendance = new DailyAttendanceInternalEntity();
        attendance.setInTime("09:30");
        attendance.setOutTime("17:45:00");
        attendance.setCheckInTime(LocalTime.of(9, 5));
        attendance.setCheckOutTime(LocalTime.of(18, 10));

        AttendanceEventWindow result = AttendanceEventTimeResolver.resolve(attendance);

        assertEquals(LocalTime.of(9, 5), result.inTime());
        assertEquals(LocalTime.of(18, 10), result.outTime());
        assertEquals("09:05", AttendanceEventTimeResolver.calculateTotalHours(result));
    }

    @Test
    void treatsOneEventAsInWithoutCreatingAnOutTime() {
        AttendanceEventWindow result = AttendanceEventTimeResolver.resolve(LocalTime.of(11, 25));

        assertEquals(LocalTime.of(11, 25), result.inTime());
        assertNull(result.outTime());
        assertNull(AttendanceEventTimeResolver.calculateTotalHours(result));
    }
}
