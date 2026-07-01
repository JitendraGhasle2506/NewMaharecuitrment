package com.maharecruitment.gov.in.common.upload;

public record ValidatedFileUpload(
        String originalFileName,
        String storedFileName,
        String extension,
        String contentType,
        long size) {
}
