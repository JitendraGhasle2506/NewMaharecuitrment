package com.maharecruitment.gov.in.web.service.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.web.exception.FileStorageException;

@Service
public class FileDownloadService {

    public ResponseEntity<Resource> preview(String fullPath, String fileName) {
        try {
            Path filePath = Paths.get(fullPath).toAbsolutePath().normalize();
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists()) {
                throw new FileStorageException("File not found.");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception ex) {
            throw new FileStorageException("Preview failed.", ex);
        }
    }

    public ResponseEntity<Resource> download(String fullPath, String fileName) {
        Path filePath = Paths.get(fullPath).toAbsolutePath().normalize();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            throw new FileStorageException("File not found.");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
