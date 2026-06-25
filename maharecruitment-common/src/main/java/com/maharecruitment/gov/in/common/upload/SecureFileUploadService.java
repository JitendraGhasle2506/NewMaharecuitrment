package com.maharecruitment.gov.in.common.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SecureFileUploadService {

    private static final Logger log = LoggerFactory.getLogger(SecureFileUploadService.class);

    private static final Pattern META_CHARACTERS = Pattern.compile("[<>:\"/\\\\|?*;&$`~]");
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] OLE_DOCUMENT_SIGNATURE = new byte[] {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private final SecureFileUploadProperties properties;

    public SecureFileUploadService(SecureFileUploadProperties properties) {
        this.properties = properties;
    }

    public ValidatedFileUpload validate(MultipartFile file, SecureFileUploadPolicy policy) {
        SecureFileUploadPolicy effectivePolicy = policy != null
                ? policy
                : SecureFileUploadPolicy.allowedExtensions("upload", properties.getAllowedExtensions());

        try {
            return validateInternal(file, effectivePolicy);
        } catch (SecureFileUploadException ex) {
            logRejectedUpload(effectivePolicy.context(), file, ex.getMessage());
            throw ex;
        }
    }

    public boolean isStoredFileAllowed(Path path, SecureFileUploadPolicy policy) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }

        try {
            String fileName = path.getFileName() != null ? path.getFileName().toString() : "";
            String extension = extractExtension(fileName).toLowerCase(Locale.ROOT);
            SecureFileUploadPolicy effectivePolicy = policy != null
                    ? policy
                    : SecureFileUploadPolicy.allowedExtensions("stored-file", properties.getAllowedExtensions());
            if (!effectivePolicy.allowedExtensions().contains(extension)
                    || isBlockedExtension(extension)
                    || !isConfiguredAllowedExtension(extension)) {
                return false;
            }

            byte[] signature = readSignature(path);
            try (InputStream inputStream = Files.newInputStream(path)) {
                return matchesMagicBytes(extension, signature, inputStream);
            }
        } catch (RuntimeException | IOException ex) {
            log.warn("Stored upload validation failed. reason={}", ex.getMessage());
            return false;
        }
    }

    public Path resolveSecureDirectory(Path baseDirectory, String... pathParts) {
        Path base = baseDirectory.toAbsolutePath().normalize();
        Path candidate = base;
        if (pathParts != null) {
            for (String pathPart : pathParts) {
                if (!StringUtils.hasText(pathPart)) {
                    continue;
                }
                candidate = candidate.resolve(pathPart.trim());
            }
        }

        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(base)) {
            throw new SecureFileUploadException("Upload path is invalid.");
        }
        return normalized;
    }

    public Path resolveSecureFile(Path uploadDirectory, String storedFileName) {
        if (!StringUtils.hasText(storedFileName) || META_CHARACTERS.matcher(storedFileName).find()) {
            throw new SecureFileUploadException("Stored file name is invalid.");
        }

        Path directory = uploadDirectory.toAbsolutePath().normalize();
        Path target = directory.resolve(storedFileName).toAbsolutePath().normalize();
        if (!target.startsWith(directory)) {
            throw new SecureFileUploadException("Upload target path is invalid.");
        }
        return target;
    }

    public void applyNonExecutableFilePermissions(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path, Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            log.debug("Unable to set POSIX permissions for uploaded file: {}", path, ex);
        }
    }

    private ValidatedFileUpload validateInternal(MultipartFile file, SecureFileUploadPolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new SecureFileUploadException("Uploaded file is required.");
        }

        String originalFileName = normalizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName).toLowerCase(Locale.ROOT);

        validateExtension(extension, policy);
        validateSize(file.getSize(), extension);
        validateDeclaredMimeType(file.getContentType(), extension);
        validateMagicBytes(file, extension);

        String storedFileName = UUID.randomUUID() + "." + extension;
        return new ValidatedFileUpload(
                originalFileName,
                storedFileName,
                extension,
                canonicalContentType(extension),
                file.getSize());
    }

    private String normalizeFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new SecureFileUploadException("Uploaded file name is required.");
        }

        String normalized = Normalizer.normalize(originalFileName.trim(), Normalizer.Form.NFKC);
        if (normalized.indexOf('\0') >= 0 || normalized.toLowerCase(Locale.ROOT).contains("%00")) {
            throw new SecureFileUploadException("Uploaded file name is invalid.");
        }
        if (normalized.length() > properties.getFileNameMaxLength()) {
            throw new SecureFileUploadException(
                    "Uploaded file name must be " + properties.getFileNameMaxLength() + " characters or less.");
        }
        if (normalized.contains("..")
                || normalized.startsWith(".")
                || normalized.endsWith(".")
                || META_CHARACTERS.matcher(normalized).find()) {
            throw new SecureFileUploadException("Uploaded file name contains unsafe characters.");
        }
        if (normalized.chars().filter(character -> character == '.').count() != 1) {
            throw new SecureFileUploadException("Uploaded file name must contain only one extension.");
        }

        return normalized;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == fileName.length() - 1) {
            throw new SecureFileUploadException("Uploaded file extension is missing.");
        }
        return fileName.substring(dotIndex + 1);
    }

    private void validateExtension(String extension, SecureFileUploadPolicy policy) {
        if (isBlockedExtension(extension)) {
            throw new SecureFileUploadException("Uploaded file type is not allowed.");
        }
        if (!isConfiguredAllowedExtension(extension) || !policy.allowedExtensions().contains(extension)) {
            throw new SecureFileUploadException("Only " + formatAllowedExtensions(policy.allowedExtensions())
                    + " files are allowed.");
        }
    }

    private boolean isBlockedExtension(String extension) {
        return properties.getBlockedExtensions().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(extension));
    }

    private boolean isConfiguredAllowedExtension(String extension) {
        return properties.getAllowedExtensions().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(extension))
                || Set.of("doc", "docx").contains(extension);
    }

    private void validateSize(long size, String extension) {
        if (size <= 0) {
            throw new SecureFileUploadException("Uploaded file is empty.");
        }

        long maxSize = properties.getMaxSizeByExtension()
                .getOrDefault(extension, properties.getDefaultMaxSize())
                .toBytes();
        if (size > maxSize) {
            throw new SecureFileUploadException("Uploaded " + extension.toUpperCase(Locale.ROOT)
                    + " file exceeds the allowed size of " + formatBytes(maxSize) + ".");
        }
    }

    private void validateDeclaredMimeType(String declaredContentType, String extension) {
        if (!StringUtils.hasText(declaredContentType)) {
            throw new SecureFileUploadException("Uploaded file content type is missing.");
        }

        String normalizedContentType = declaredContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        Set<String> allowedMimeTypes = properties.getAllowedMimeTypes()
                .getOrDefault(extension, List.of())
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        if (!allowedMimeTypes.contains(normalizedContentType)) {
            throw new SecureFileUploadException("Uploaded file content type is not allowed.");
        }
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try (InputStream signatureInputStream = file.getInputStream();
                InputStream contentInputStream = file.getInputStream()) {
            byte[] signature = signatureInputStream.readNBytes(16);
            if (!matchesMagicBytes(extension, signature, contentInputStream)) {
                throw new SecureFileUploadException("Uploaded file content does not match its extension.");
            }
        } catch (IOException ex) {
            throw new SecureFileUploadException("Unable to inspect uploaded file content.", ex);
        }
    }

    private boolean matchesMagicBytes(String extension, byte[] signature, InputStream contentInputStream)
            throws IOException {
        return switch (extension) {
            case "pdf" -> startsWith(signature, "%PDF".getBytes(StandardCharsets.US_ASCII));
            case "jpg", "jpeg" -> signature.length >= 3
                    && (signature[0] & 0xFF) == 0xFF
                    && (signature[1] & 0xFF) == 0xD8
                    && (signature[2] & 0xFF) == 0xFF;
            case "png" -> startsWith(signature, PNG_SIGNATURE);
            case "doc" -> startsWith(signature, OLE_DOCUMENT_SIGNATURE);
            case "docx" -> startsWith(signature, new byte[] { 0x50, 0x4B, 0x03, 0x04 })
                    && containsDocxEntries(contentInputStream);
            default -> false;
        };
    }

    private boolean containsDocxEntries(InputStream inputStream) throws IOException {
        boolean hasContentTypes = false;
        boolean hasWordDocument = false;
        int inspectedEntries = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null && inspectedEntries < 100) {
                inspectedEntries++;
                String entryName = entry.getName();
                if ("[Content_Types].xml".equals(entryName)) {
                    hasContentTypes = true;
                }
                if ("word/document.xml".equals(entryName) || entryName.startsWith("word/")) {
                    hasWordDocument = true;
                }
                if (hasContentTypes && hasWordDocument) {
                    return true;
                }
            }
        }

        return false;
    }

    private byte[] readSignature(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return inputStream.readNBytes(16);
        }
    }

    private boolean startsWith(byte[] source, byte[] expectedPrefix) {
        if (source.length < expectedPrefix.length) {
            return false;
        }
        for (int index = 0; index < expectedPrefix.length; index++) {
            if (source[index] != expectedPrefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String canonicalContentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String formatAllowedExtensions(Set<String> extensions) {
        return extensions.stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String formatBytes(long bytes) {
        long oneMb = 1024L * 1024L;
        long oneKb = 1024L;
        if (bytes >= oneMb && bytes % oneMb == 0) {
            return (bytes / oneMb) + " MB";
        }
        if (bytes >= oneKb && bytes % oneKb == 0) {
            return (bytes / oneKb) + " KB";
        }
        return bytes + " bytes";
    }

    private void logRejectedUpload(String context, MultipartFile file, String reason) {
        String extension = "unknown";
        long size = -1L;
        if (file != null) {
            size = file.getSize();
            String originalFileName = file.getOriginalFilename();
            if (StringUtils.hasText(originalFileName) && originalFileName.lastIndexOf('.') >= 0) {
                extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                        .toLowerCase(Locale.ROOT);
                if (extension.length() > 16) {
                    extension = "invalid";
                }
            }
        }

        log.warn(
                "Rejected file upload. context={}, extension={}, size={}, reason={}",
                context,
                extension,
                size,
                reason);
    }

    @Override
    public String toString() {
        return "SecureFileUploadService{allowedExtensions="
                + Arrays.toString(properties.getAllowedExtensions().toArray())
                + "}";
    }
}
