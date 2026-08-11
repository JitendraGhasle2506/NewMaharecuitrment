package com.maharecruitment.gov.in.web.event.mobile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceUpdateClient;

@ExtendWith(MockitoExtension.class)
class MobileAttendanceUpdateListenerTest {

    private static final LocalDate ATTENDANCE_DATE = LocalDate.of(2026, 8, 11);

    @Mock
    private InternalAttendanceUpdateClient attendanceUpdateClient;

    @InjectMocks
    private MobileAttendanceUpdateListener listener;

    @Test
    void routesCheckInAndCheckOutToTheirDedicatedClientOperations() {
        listener.onCheckInRecorded(new MobileCheckInRecordedEvent(
                "MahaIT0693",
                ATTENDANCE_DATE,
                LocalTime.of(10, 0)));
        listener.onCheckOutRecorded(new MobileCheckOutRecordedEvent(
                "MahaIT0693",
                ATTENDANCE_DATE,
                LocalTime.of(11, 1)));

        verify(attendanceUpdateClient).updateCheckIn(
                "MahaIT0693",
                ATTENDANCE_DATE,
                LocalTime.of(10, 0));
        verify(attendanceUpdateClient).updateCheckOut(
                "MahaIT0693",
                ATTENDANCE_DATE,
                LocalTime.of(11, 1));
    }

    @Test
    void upstreamFailureDoesNotFailTheCommittedMobilePunch() {
        doThrow(new RuntimeException("upstream unavailable"))
                .when(attendanceUpdateClient)
                .updateCheckIn("MahaIT0693", ATTENDANCE_DATE, LocalTime.of(10, 0));

        assertThatCode(() -> listener.onCheckInRecorded(new MobileCheckInRecordedEvent(
                "MahaIT0693",
                ATTENDANCE_DATE,
                LocalTime.of(10, 0))))
                .doesNotThrowAnyException();
    }
}
