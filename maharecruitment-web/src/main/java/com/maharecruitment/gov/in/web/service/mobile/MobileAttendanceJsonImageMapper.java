package com.maharecruitment.gov.in.web.service.mobile;

import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceMarkJsonRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileAttendanceJsonRequest;

@Component
public class MobileAttendanceJsonImageMapper {

    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final int MAX_BASE64_CHARACTERS = 8_000_000;

    public MultipartFile toMultipartFile(MobileAttendanceJsonRequest request) {
        if (request == null || !StringUtils.hasText(request.imageBase64())) {
            throw invalidImage("Image data is required.");
        }

        return toMultipartFile(request.imageBase64(), request.imageFileName(), request.imageContentType());
    }

    public MultipartFile toMultipartFile(MobileAttendanceMarkJsonRequest request) {
        if (request == null || !StringUtils.hasText(request.imageBase64())) {
            throw invalidImage("Image data is required.");
        }

        return toMultipartFile(request.imageBase64(), request.imageFileName(), request.imageContentType());
    }

    private MultipartFile toMultipartFile(String imageBase64, String imageFileName, String imageContentType) {
        ImagePayload payload = parsePayload(imageBase64, imageContentType);
        String fileName = resolveFileName(imageFileName, payload.contentType());
        byte[] bytes = decode(payload.base64Data());
        return new Base64MultipartFile("image", fileName, payload.contentType(), bytes);
    }

    private ImagePayload parsePayload(String rawImageData, String explicitContentType) {
        String normalizedImageData = rawImageData.trim();
        if (normalizedImageData.length() > MAX_BASE64_CHARACTERS) {
            throw invalidImage("Image data is too large.");
        }

        Matcher matcher = DATA_URI_PATTERN.matcher(normalizedImageData);
        if (matcher.matches()) {
            return new ImagePayload(normalizeContentType(matcher.group(1)), matcher.group(2).trim());
        }

        return new ImagePayload(normalizeContentType(explicitContentType), normalizedImageData);
    }

    private byte[] decode(String base64Data) {
        try {
            return Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException ex) {
            try {
                return Base64.getMimeDecoder().decode(base64Data);
            } catch (IllegalArgumentException mimeEx) {
                throw invalidImage("Image data must be valid Base64.");
            }
        }
    }

    private String resolveFileName(String requestedFileName, String contentType) {
        if (StringUtils.hasText(requestedFileName)) {
            return requestedFileName.trim();
        }
        return "attendance." + extensionFor(contentType);
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "image/jpeg";
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        if ("image/jpeg".equals(normalized) || "image/png".equals(normalized)) {
            return normalized;
        }
        throw invalidImage("Image content type must be image/jpeg or image/png.");
    }

    private String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? "png" : "jpg";
    }

    private MobileAttendanceException invalidImage(String message) {
        return new MobileAttendanceException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", message);
    }

    private record ImagePayload(String contentType, String base64Data) {
    }
}
