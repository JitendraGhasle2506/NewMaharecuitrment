package com.maharecruitment.gov.in.attendance.service;

import java.util.Locale;

import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;

public final class AttendanceStatusResolver {

    private AttendanceStatusResolver() {
    }

    public static String resolveDisplayStatus(DailyAttendanceInternalEntity attendance) {
        if (attendance == null) {
            return null;
        }
        if (AttendanceEventTimeResolver.resolve(attendance).hasAttendanceEvent()) {
            return "PRESENT";
        }
        return resolveDisplayStatus(attendance.getStatus(), attendance.getInTime(), attendance.getOutTime());
    }

    public static String resolveDisplayStatus(String rawStatus, String inTime, String outTime) {
        String normalizedStatus = normalizeStatus(rawStatus);
        if (!StringUtils.hasText(normalizedStatus)) {
            return hasAnyPunchTime(inTime, outTime) ? "PRESENT" : null;
        }
        if (isWeekOff(normalizedStatus) && hasCompletePunchTime(inTime, outTime)) {
            return "PRESENT";
        }
        return normalizedStatus;
    }

    public static String resolveStatusCode(DailyAttendanceInternalEntity attendance) {
        if (attendance == null) {
            return "";
        }
        return toStatusCode(resolveDisplayStatus(attendance));
    }

    public static String resolveStatusCode(String rawStatus) {
        return resolveStatusCode(rawStatus, null, null);
    }

    public static String resolveStatusCode(String rawStatus, String inTime, String outTime) {
        return toStatusCode(resolveDisplayStatus(rawStatus, inTime, outTime));
    }

    private static String toStatusCode(String displayStatus) {
        if (!StringUtils.hasText(displayStatus)) {
            return "";
        }

        switch (displayStatus) {
            case "PRESENT":
                return "P";
            case "ABSENT":
                return "A";
            case "WEEK_OFF":
                return "W";
            case "HOLIDAY":
                return "H";
            case "LEAVE":
                return "L";
            case "TOUR":
                return "T";
            default:
                return displayStatus;
        }
    }

    private static String normalizeStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return null;
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ENGLISH);
        switch (normalizedStatus) {
            case "P":
            case "PRESENT":
                return "PRESENT";
            case "A":
            case "ABSENT":
                return "ABSENT";
            case "W":
            case "WO":
            case "WEEK_OFF":
                return "WEEK_OFF";
            case "H":
            case "HOLIDAY":
                return "HOLIDAY";
            case "L":
            case "LEAVE":
                return "LEAVE";
            case "T":
            case "TOUR":
                return "TOUR";
            default:
                return normalizedStatus;
        }
    }

    private static boolean isWeekOff(String status) {
        return "WEEK_OFF".equals(status);
    }

    private static boolean hasAnyPunchTime(String inTime, String outTime) {
        return StringUtils.hasText(inTime) || StringUtils.hasText(outTime);
    }

    private static boolean hasCompletePunchTime(String inTime, String outTime) {
        return StringUtils.hasText(inTime) && StringUtils.hasText(outTime);
    }
}
