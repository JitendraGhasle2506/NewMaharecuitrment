package com.maharecruitment.gov.in.department.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.common.upload.SecureFileUploadException;
import com.maharecruitment.gov.in.common.upload.SecureFileUploadPolicy;
import com.maharecruitment.gov.in.common.upload.SecureFileUploadService;
import com.maharecruitment.gov.in.common.upload.ValidatedFileUpload;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.model.StoredDocument;

@Service
public class DepartmentPaymentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentPaymentStorageService.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final SecureFileUploadPolicy PAYMENT_RECEIPT_POLICY =
            SecureFileUploadPolicy.allowedExtensions("department-payment-receipt", Set.of("pdf", "jpg", "jpeg", "png"));

    private final Path baseDirectory;
    private final SecureFileUploadService secureFileUploadService;

    public DepartmentPaymentStorageService(
            @Value("${department.payment.receipt.base-path:${secure.upload.base-path:${user.home}/uploaded-files}/receipts}") String basePath,
            SecureFileUploadService secureFileUploadService) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
        this.secureFileUploadService = secureFileUploadService;
    }

    public StoredDocument storePaymentReceipt(MultipartFile file, String existingPath) {
        ValidatedFileUpload validatedFile = validateFile(file);

        try {
            Path uploadDirectory = secureFileUploadService.resolveSecureDirectory(
                    baseDirectory,
                    "department/payment/receipt",
                    LocalDate.now().format(YEAR_MONTH_FORMAT));
            Files.createDirectories(uploadDirectory);

            Path targetPath = secureFileUploadService.resolveSecureFile(uploadDirectory, validatedFile.storedFileName());

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            secureFileUploadService.applyNonExecutableFilePermissions(targetPath);
            removeManagedFileQuietly(existingPath);

            log.info("Payment receipt document stored. path={}", targetPath);
            return StoredDocument.builder()
                    .originalFileName(validatedFile.originalFileName())
                    .fullPath(targetPath.toString())
                    .contentType(validatedFile.contentType())
                    .fileSize(validatedFile.size())
                    .build();
        } catch (IOException ex) {
            log.error("Unable to store payment receipt document.", ex);
            throw new DepartmentApplicationException("Unable to store payment receipt document: " + ex.getMessage());
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

    private ValidatedFileUpload validateFile(MultipartFile file) {
        try {
            return secureFileUploadService.validate(file, PAYMENT_RECEIPT_POLICY);
        } catch (SecureFileUploadException ex) {
            throw new DepartmentApplicationException(ex.getMessage());
        }
    }
}
