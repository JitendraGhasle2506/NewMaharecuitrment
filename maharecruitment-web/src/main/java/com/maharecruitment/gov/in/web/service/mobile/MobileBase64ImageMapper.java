package com.maharecruitment.gov.in.web.service.mobile;

import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MobileBase64ImageMapper {

    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final int MAX_BASE64_CHARACTERS = 8_000_000;

    public MultipartFile toMultipartFile(
            String rawImageData,
            String requestedFileName,
            String explicitContentType,
            String fieldName,
            String defaultBaseFileName) {
        if (!StringUtils.hasText(rawImageData)) {
            throw invalidImage("Image data is required.");
        }

        ImagePayload payload = parsePayload(rawImageData, explicitContentType);
        String fileName = resolveFileName(requestedFileName, payload.contentType(), defaultBaseFileName);
        byte[] bytes = decode(payload.base64Data());
        return new Base64MultipartFile(resolveFieldName(fieldName), fileName, payload.contentType(), bytes);
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

    private String resolveFileName(String requestedFileName, String contentType, String defaultBaseFileName) {
        if (StringUtils.hasText(requestedFileName)) {
            return requestedFileName.trim();
        }
        String baseFileName = StringUtils.hasText(defaultBaseFileName) ? defaultBaseFileName.trim() : "image";
        return baseFileName + "." + extensionFor(contentType);
    }

    private String resolveFieldName(String fieldName) {
        return StringUtils.hasText(fieldName) ? fieldName.trim() : "image";
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

    private InvalidBase64ImageException invalidImage(String message) {
        return new InvalidBase64ImageException(message);
    }

    public static class InvalidBase64ImageException extends RuntimeException {

        public InvalidBase64ImageException(String message) {
            super(message);
        }
    }

    private record ImagePayload(String contentType, String base64Data) {
    }
}
