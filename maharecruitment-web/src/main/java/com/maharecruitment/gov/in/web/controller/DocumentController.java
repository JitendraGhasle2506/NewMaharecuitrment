package com.maharecruitment.gov.in.web.controller;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final FileStorageService fileStorageService;

    public DocumentController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewDocument(@RequestParam("path") String encodedPath) {
        try {
            String decodedPath = new String(Base64.getDecoder().decode(encodedPath), StandardCharsets.UTF_8);
            log.info("Request to view document at: {}", decodedPath);

            if (!fileStorageService.isManagedPath(decodedPath)) {
                log.warn("Unauthorized or invalid file path access attempted: {}", decodedPath);
                return ResponseEntity.status(403).build();
            }

            Path path = Paths.get(decodedPath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("File not found or not readable: {}", decodedPath);
                return ResponseEntity.notFound().build();
            }

            String contentType = URLConnection.guessContentTypeFromName(path.getFileName().toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving document", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
