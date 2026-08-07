package com.maharecruitment.gov.in.attendance.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;

public final class AttendanceEventTimeResolver {

    private static final DateTimeFormatter FLEXIBLE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalEnd()
            .toFormatter();
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private AttendanceEventTimeResolver() {
    }

    public static AttendanceEventWindow resolve(DailyAttendanceInternalEntity attendance) {
        if (attendance == null) {
            return AttendanceEventWindow.empty();
        }
        return resolve(
                parse(attendance.getInTime()),
                parse(attendance.getOutTime()),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime());
    }

    public static AttendanceEventWindow resolve(LocalTime... eventTimes) {
        LocalTime[] orderedEvents = Stream.of(eventTimes == null ? new LocalTime[0] : eventTimes)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toArray(LocalTime[]::new);
        if (orderedEvents.length == 0) {
            return AttendanceEventWindow.empty();
        }
        return new AttendanceEventWindow(
                orderedEvents[0],
                orderedEvents.length > 1 ? orderedEvents[orderedEvents.length - 1] : null);
    }

    public static LocalTime parse(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), FLEXIBLE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static String format(LocalTime value) {
        return value == null ? null : DISPLAY_TIME_FORMATTER.format(value);
    }

    public static String calculateTotalHours(AttendanceEventWindow window) {
        if (window == null || window.inTime() == null || window.outTime() == null) {
            return null;
        }
        Duration duration = Duration.between(window.inTime(), window.outTime());
        return "%02d:%02d".formatted(duration.toHours(), duration.toMinutesPart());
    }

    public record AttendanceEventWindow(LocalTime inTime, LocalTime outTime) {

        private static AttendanceEventWindow empty() {
            return new AttendanceEventWindow(null, null);
        }

        public boolean hasAttendanceEvent() {
            return inTime != null;
        }
    }
}
