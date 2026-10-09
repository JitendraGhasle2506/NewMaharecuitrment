package com.maharecruitment.gov.in.attendance.dto;

public record CommonCalendarPhotoUpload(
        String originalName,
        String storedName,
        String filePath,
        String contentType,
        long fileSize) {
}
