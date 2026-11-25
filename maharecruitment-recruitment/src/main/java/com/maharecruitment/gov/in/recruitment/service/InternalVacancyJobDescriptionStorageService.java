package com.maharecruitment.gov.in.recruitment.service;

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
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.model.StoredInternalVacancyApprovalDocument;

/**
 * Stores the optional job description document (PDF or Word) attached to an
 * internal vacancy opening.
 */
@Service
public class InternalVacancyJobDescriptionStorageService {

    private static final Logger log = LoggerFactory.getLogger(InternalVacancyJobDescriptionStorageService.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String UNAVAILABLE_MESSAGE = "Job description document is unavailable.";
    private static final SecureFileUploadPolicy JOB_DESCRIPTION_POLICY = SecureFileUploadPolicy
            .allowedExtensions("internal-vacancy-job-description", Set.of("pdf", "doc", "docx"));

    private final Path baseDirectory;
    private final SecureFileUploadService secureFileUploadService;

    public InternalVacancyJobDescriptionStorageService(
            @Value("${recruitment.internal-vacancy.job-description.base-path:${secure.upload.base-path:${user.home}/uploaded-files}/internal-vacancy-job-descriptions}") String basePath,
            SecureFileUploadService secureFileUploadService) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
        this.secureFileUploadService = secureFileUploadService;
    }

    public StoredInternalVacancyApprovalDocument store(MultipartFile file) {
        ValidatedFileUpload validatedFile = validate(file);

        try {
            Path uploadDirectory = secureFileUploadService.resolveSecureDirectory(
                    baseDirectory,
                    LocalDate.now().format(YEAR_MONTH_FORMAT));
            Files.createDirectories(uploadDirectory);

            Path targetPath = secureFileUploadService.resolveSecureFile(
                    uploadDirectory,
                    validatedFile.storedFileName());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            secureFileUploadService.applyNonExecutableFilePermissions(targetPath);

            log.info("Internal vacancy job description stored. path={}", targetPath);
            return StoredInternalVacancyApprovalDocument.builder()
                    .originalFileName(validatedFile.originalFileName())
                    .fullPath(targetPath.toString())
                    .contentType(validatedFile.contentType())
                    .fileSize(validatedFile.size())
                    .build();
        } catch (IOException | SecureFileUploadException ex) {
            log.error("Unable to store internal vacancy job description.", ex);
            throw new RecruitmentNotificationException("Unable to store the job description document.");
        }
    }

    public Resource loadAsResource(String fullPath) {
        Path path = resolveManagedFile(fullPath);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RecruitmentNotificationException(UNAVAILABLE_MESSAGE);
            }
            return resource;
        } catch (IOException ex) {
            throw new RecruitmentNotificationException("Unable to access the job description document.");
        }
    }

    public void removeManagedFileQuietly(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return;
        }

        try {
            Path path = Paths.get(fullPath).toAbsolutePath().normalize();
            if (path.startsWith(baseDirectory)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Unable to remove internal vacancy job description. path={}", fullPath, ex);
        }
    }

    private Path resolveManagedFile(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            throw new RecruitmentNotificationException(UNAVAILABLE_MESSAGE);
        }

        try {
            Path path = Paths.get(fullPath).toAbsolutePath().normalize();
            if (!path.startsWith(baseDirectory)
                    || !Files.exists(path)
                    || !Files.isRegularFile(path)
                    || !secureFileUploadService.isStoredFileAllowed(path, JOB_DESCRIPTION_POLICY)) {
                throw new RecruitmentNotificationException(UNAVAILABLE_MESSAGE);
            }
            return path;
        } catch (RuntimeException ex) {
            if (ex instanceof RecruitmentNotificationException recruitmentException) {
                throw recruitmentException;
            }
            throw new RecruitmentNotificationException(UNAVAILABLE_MESSAGE);
        }
    }

    private ValidatedFileUpload validate(MultipartFile file) {
        try {
            return secureFileUploadService.validate(file, JOB_DESCRIPTION_POLICY);
        } catch (SecureFileUploadException ex) {
            throw new RecruitmentNotificationException(ex.getMessage());
        }
    }
}
