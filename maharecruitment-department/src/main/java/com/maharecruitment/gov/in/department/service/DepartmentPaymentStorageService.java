package com.maharecruitment.gov.in.department.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.model.StoredDocument;

@Service
public class DepartmentPaymentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentPaymentStorageService.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path baseDirectory;
    private final long maxFileSizeInBytes;
    private final Set<String> allowedContentTypes;

    public DepartmentPaymentStorageService(
            @Value("${department.payment.receipt.base-path:uploads}") String basePath,
            @Value("${department.payment.receipt.max-size-bytes:5242880}") long maxFileSizeInBytes,
            @Value("${department.payment.receipt.allowed-types:application/pdf,image/jpeg,image/png}") String allowedTypes) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
        this.maxFileSizeInBytes = maxFileSizeInBytes;
        this.allowedContentTypes = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(type -> !type.isBlank())
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public StoredDocument storePaymentReceipt(MultipartFile file, String existingPath) {
        validateFile(file);

        try {
            Path uploadDirectory = baseDirectory
                    .resolve("department/payment/receipt")
                    .resolve(LocalDate.now().format(YEAR_MONTH_FORMAT))
                    .normalize();
            Files.createDirectories(uploadDirectory);

            String originalFileName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "receipt" : file.getOriginalFilename());
            if (originalFileName.contains("..")) {
                throw new DepartmentApplicationException("Invalid receipt file name.");
            }

            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            removeManagedFileQuietly(existingPath);

            log.info("Payment receipt document stored. path={}", targetPath);
            return StoredDocument.builder()
                    .originalFileName(originalFileName)
                    .fullPath(targetPath.toString())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException ex) {
            log.error("Unable to store payment receipt document.", ex);
            throw new DepartmentApplicationException("Unable to store payment receipt document.");
        }
    }

    public Resource loadAsResource(String fullPath) {
        if (!isManagedPath(fullPath)) {
            throw new DepartmentApplicationException("Receipt file is unavailable.");
        }

        try {
            Path path = Paths.get(fullPath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new DepartmentApplicationException("Receipt file is unavailable.");
            }
            return resource;
        } catch (IOException ex) {
            throw new DepartmentApplicationException("Unable to access receipt file.");
        }
    }

    public void removeManagedFileQuietly(String fullPath) {
        if (!isManagedPath(fullPath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(fullPath).toAbsolutePath().normalize());
        } catch (IOException ex) {
            log.warn("Failed to delete managed receipt file: {}", fullPath, ex);
        }
    }

    public boolean isManagedPath(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return false;
        }

        try {
            Path candidate = Paths.get(fullPath).toAbsolutePath().normalize();
            return candidate.startsWith(baseDirectory) && Files.exists(candidate) && Files.isRegularFile(candidate);
        } catch (RuntimeException ex) {
            log.warn("Invalid managed path supplied: {}", fullPath, ex);
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DepartmentApplicationException("Receipt document is required.");
        }

        if (file.getSize() > maxFileSizeInBytes) {
            throw new DepartmentApplicationException("Receipt file size must be 5 MB or less.");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new DepartmentApplicationException("Receipt file type is missing.");
        }

        if (!allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new DepartmentApplicationException("Only PDF, JPEG or PNG receipt files are allowed.");
        }
    }
}
