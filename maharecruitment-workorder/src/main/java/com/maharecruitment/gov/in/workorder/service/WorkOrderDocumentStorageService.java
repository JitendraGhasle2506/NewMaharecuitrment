package com.maharecruitment.gov.in.workorder.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.workorder.exception.WorkOrderException;
import com.maharecruitment.gov.in.workorder.service.model.GeneratedWorkOrderDocument;

@Service
public class WorkOrderDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderDocumentStorageService.class);

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path baseDirectory;

    public WorkOrderDocumentStorageService(
            @Value("${workorder.document.base-path:uploads}") String basePath) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
    }

    public String store(GeneratedWorkOrderDocument document) {
        if (document == null || document.bytes() == null || document.bytes().length == 0) {
            throw new WorkOrderException("Generated work-order document is empty.");
        }

        try {
            Path uploadDirectory = baseDirectory
                    .resolve("workorders/generated")
                    .resolve(LocalDate.now().format(YEAR_MONTH_FORMAT))
                    .normalize();
            Files.createDirectories(uploadDirectory);

            String originalFileName = sanitizeFileName(document.originalFileName());
            Path targetPath = uploadDirectory.resolve(originalFileName).normalize();
            if (!targetPath.startsWith(uploadDirectory)) {
                throw new WorkOrderException("Invalid generated work-order file name.");
            }

            Files.write(targetPath, document.bytes());
            log.info("Work-order document stored. path={}", targetPath);
            return targetPath.toString();
        } catch (IOException ex) {
            log.error("Unable to store generated work-order document.", ex);
            throw new WorkOrderException("Unable to store generated work-order document.", ex);
        }
    }

    public Resource loadAsResource(String fullPath) {
        if (!isManagedPath(fullPath)) {
            throw new WorkOrderException("Work-order document is unavailable.");
        }

        try {
            Path path = Paths.get(fullPath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new WorkOrderException("Work-order document is unavailable.");
            }
            return resource;
        } catch (IOException ex) {
            throw new WorkOrderException("Unable to access work-order document.", ex);
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
            log.warn("Invalid work-order document path supplied: {}", fullPath, ex);
            return false;
        }
    }

    private String sanitizeFileName(String originalFileName) {
        String fileName = StringUtils.hasText(originalFileName)
                ? originalFileName.trim()
                : "work-order.pdf";

        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.contains("..")) {
            throw new WorkOrderException("Invalid generated work-order file name.");
        }
        return sanitized;
    }
}
