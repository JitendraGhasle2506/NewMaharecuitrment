package com.maharecruitment.gov.in.recruitment.controller;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyApprovalDocumentView;

/**
 * Builds the download response for an internal vacancy job description.
 * PDFs open in the browser; Word files are downloaded.
 */
public final class JobDescriptionResponseFactory {

    private static final MediaType DOC_MEDIA_TYPE = MediaType.parseMediaType("application/msword");
    private static final MediaType DOCX_MEDIA_TYPE = MediaType
            .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private JobDescriptionResponseFactory() {
    }

    public static ResponseEntity<Resource> build(InternalVacancyApprovalDocumentView document) {
        String fileName = document.getOriginalFileName();
        MediaType mediaType = resolveMediaType(fileName);
        boolean inline = MediaType.APPLICATION_PDF.equals(mediaType);

        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff");
        if (document.getFileSize() != null) {
            response.contentLength(document.getFileSize());
        }
        return response.body(document.getResource());
    }

    private static MediaType resolveMediaType(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim().toLowerCase(Locale.ROOT) : "";
        if (normalized.endsWith(".docx")) {
            return DOCX_MEDIA_TYPE;
        }
        if (normalized.endsWith(".doc")) {
            return DOC_MEDIA_TYPE;
        }
        return MediaType.APPLICATION_PDF;
    }
}
