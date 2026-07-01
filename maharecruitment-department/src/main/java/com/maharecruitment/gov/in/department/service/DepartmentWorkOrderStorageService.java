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
public class DepartmentWorkOrderStorageService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentWorkOrderStorageService.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final SecureFileUploadPolicy WORK_ORDER_POLICY =
            SecureFileUploadPolicy.allowedExtensions("department-work-order", Set.of("pdf", "doc", "docx"));

    private final Path baseDirectory;
    private final SecureFileUploadService secureFileUploadService;

    public DepartmentWorkOrderStorageService(
            @Value("${department.manpower.work-order.base-path:${secure.upload.base-path:${user.home}/uploaded-files}/work-orders}") String basePath,
            SecureFileUploadService secureFileUploadService) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
        this.secureFileUploadService = secureFileUploadService;
    }

    public StoredDocument storeWorkOrder(MultipartFile file, String existingPath) {
        ValidatedFileUpload validatedFile = validateFile(file);

        try {
            Path uploadDirectory = secureFileUploadService.resolveSecureDirectory(
                    baseDirectory,
                    "department-manpower/work-order",
                    LocalDate.now().format(YEAR_MONTH_FORMAT));
            Files.createDirectories(uploadDirectory);

            Path targetPath = secureFileUploadService.resolveSecureFile(uploadDirectory, validatedFile.storedFileName());

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            secureFileUploadService.applyNonExecutableFilePermissions(targetPath);
            removeManagedFileQuietly(existingPath);

            log.info("Work-order document stored. path={}", targetPath);
            return StoredDocument.builder()
                    .originalFileName(validatedFile.originalFileName())
                    .fullPath(targetPath.toString())
                    .contentType(validatedFile.contentType())
                    .fileSize(validatedFile.size())
                    .build();
        } catch (IOException ex) {
            log.error("Unable to store work-order document.", ex);
            throw new DepartmentApplicationException("Unable to store work-order document.");
        }
    }

    public Resource loadAsResource(String fullPath) {
        if (!isManagedPath(fullPath)) {
            throw new DepartmentApplicationException("Work-order file is unavailable.");
        }

        try {
            Path path = Paths.get(fullPath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new DepartmentApplicationException("Work-order file is unavailable.");
            }
            return resource;
        } catch (IOException ex) {
            throw new DepartmentApplicationException("Unable to access work-order file.");
        }
    }

    public void removeManagedFileQuietly(String fullPath) {
        if (!isManagedPath(fullPath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(fullPath).toAbsolutePath().normalize());
        } catch (IOException ex) {
            log.warn("Failed to delete managed work-order file: {}", fullPath, ex);
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
            return secureFileUploadService.validate(file, WORK_ORDER_POLICY);
        } catch (SecureFileUploadException ex) {
            throw new DepartmentApplicationException(ex.getMessage());
        }
    }
}
