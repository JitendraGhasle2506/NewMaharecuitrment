package com.maharecruitment.gov.in.web.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.exception.FileStorageException;
import com.maharecruitment.gov.in.web.properties.FileUploadProperties;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileUploadProperties properties;

    public FileStorageService(FileUploadProperties properties) {
        this.properties = properties;
    }

    public FileUploadResult store(MultipartFile file, String module) {
        validate(file);

        try {
            Path baseDir = Paths.get(properties.getBasePath())
                    .toAbsolutePath()
                    .normalize();

            String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadDir = baseDir.resolve(module).resolve(yearMonth).normalize();
            Files.createDirectories(uploadDir);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                    ? "document"
                    : file.getOriginalFilename());

            if (originalFileName.contains("..")) {
                throw new FileStorageException("Invalid file name.");
            }

            String storedFileName = UUID.randomUUID() + "_" + originalFileName;
            Path targetLocation = uploadDir.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully at {}", targetLocation);

            return new FileUploadResult(
                    originalFileName,
                    storedFileName,
                    targetLocation.toString(),
                    file.getContentType(),
                    file.getSize());
        } catch (IOException ex) {
            log.error("File upload failed", ex);
            throw new FileStorageException("File upload failed.", ex);
        }
    }

    public void deleteQuietly(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(fullPath).toAbsolutePath().normalize());
        } catch (IOException ex) {
            log.warn("Unable to delete file {}", fullPath, ex);
        }
    }

    public boolean isManagedPath(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return false;
        }

        try {
            Path baseDir = Paths.get(properties.getBasePath())
                    .toAbsolutePath()
                    .normalize();
            Path candidate = Paths.get(fullPath)
                    .toAbsolutePath()
                    .normalize();

            return candidate.startsWith(baseDir) && Files.exists(candidate) && Files.isRegularFile(candidate);
        } catch (RuntimeException ex) {
            log.warn("Invalid managed file path check for {}", fullPath, ex);
            return false;
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty.");
        }

        if (file.getSize() > properties.getMaxSize().toBytes()) {
            throw new FileStorageException("File size exceeds the allowed limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileStorageException("File content type is missing.");
        }

        boolean supported = properties.getAllowedTypes().stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .anyMatch(type -> type.equals(contentType.toLowerCase(Locale.ROOT)));

        if (!supported) {
            throw new FileStorageException("Invalid file type.");
        }
    }
}
