package com.maharecruitment.gov.in.web.dto;

public record FileUploadResult(
        String originalFileName,
        String storedFileName,
        String fullPath,
        String contentType,
        long size) {
}
