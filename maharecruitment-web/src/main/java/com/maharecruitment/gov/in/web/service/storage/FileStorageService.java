package com.maharecruitment.gov.in.web.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.common.upload.SecureFileUploadException;
import com.maharecruitment.gov.in.common.upload.SecureFileUploadPolicy;
import com.maharecruitment.gov.in.common.upload.SecureFileUploadService;
import com.maharecruitment.gov.in.common.upload.ValidatedFileUpload;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.exception.FileStorageException;
import com.maharecruitment.gov.in.web.properties.FileUploadProperties;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> DEFAULT_DOCUMENT_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final int STORED_FILE_VALIDATION_CACHE_SIZE = 512;

    private final FileUploadProperties properties;
    private final SecureFileUploadService secureFileUploadService;
    private final Map<StoredFileValidationKey, Boolean> storedFileValidationCache =
            Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<StoredFileValidationKey, Boolean> eldest) {
                    return size() > STORED_FILE_VALIDATION_CACHE_SIZE;
                }
            });

    public FileStorageService(
            FileUploadProperties properties,
            SecureFileUploadService secureFileUploadService) {
        this.properties = properties;
        this.secureFileUploadService = secureFileUploadService;
    }

    public FileUploadResult store(MultipartFile file, String module) {
        ValidatedFileUpload validatedFile = validate(file, module);

        try {
            Path baseDir = Paths.get(properties.getBasePath())
                    .toAbsolutePath()
                    .normalize();

            String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadDir = secureFileUploadService.resolveSecureDirectory(baseDir, module, yearMonth);
            Files.createDirectories(uploadDir);

            Path targetLocation = secureFileUploadService.resolveSecureFile(
                    uploadDir,
                    validatedFile.storedFileName());

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            secureFileUploadService.applyNonExecutableFilePermissions(targetLocation);
            log.info("File stored successfully at {}", targetLocation);

            return new FileUploadResult(
                    validatedFile.originalFileName(),
                    validatedFile.storedFileName(),
                    targetLocation.toString(),
                    validatedFile.contentType(),
                    validatedFile.size());
        } catch (SecureFileUploadException ex) {
            throw new FileStorageException(ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("File upload failed", ex);
            throw new FileStorageException("File upload failed.", ex);
        }
    }

    public void deleteQuietly(String fullPath) {
        if (!isManagedPath(fullPath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(fullPath).toAbsolutePath().normalize());
        } catch (IOException ex) {
            log.warn("Unable to delete file {}", fullPath, ex);
        }
    }

    public boolean isManagedPath(String fullPath) {
        return resolveManagedPath(fullPath).isPresent();
    }

    public Optional<Path> resolveManagedPath(String fullPath) {
        if (fullPath == null || fullPath.isBlank()) {
            return Optional.empty();
        }

        try {
            Path baseDir = Paths.get(properties.getBasePath())
                    .toAbsolutePath()
                    .normalize();
            Path requestedPath = Paths.get(fullPath.trim());
            Path candidate = requestedPath.isAbsolute()
                    ? requestedPath.toAbsolutePath().normalize()
                    : baseDir.resolve(requestedPath).normalize();

            if (candidate.startsWith(baseDir) && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Invalid managed file path check for {}", fullPath, ex);
            return Optional.empty();
        }
    }

    public boolean isManagedFileAllowed(String fullPath, String module) {
        Optional<Path> managedPath = resolveManagedPath(fullPath);
        if (managedPath.isEmpty()) {
            return false;
        }

        Path path = managedPath.get();
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            StoredFileValidationKey cacheKey = new StoredFileValidationKey(
                    path,
                    module == null ? "" : module.toLowerCase(Locale.ROOT),
                    attributes.size(),
                    attributes.lastModifiedTime().toMillis());
            Boolean cachedResult = storedFileValidationCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }

            boolean allowed = secureFileUploadService.isStoredFileAllowed(path, policyForModule(module));
            storedFileValidationCache.put(cacheKey, allowed);
            return allowed;
        } catch (IOException ex) {
            log.warn("Unable to read stored file attributes for {}", path, ex);
            return false;
        }
    }

    private ValidatedFileUpload validate(MultipartFile file, String module) {
        try {
            return secureFileUploadService.validate(file, policyForModule(module));
        } catch (SecureFileUploadException ex) {
            throw new FileStorageException(ex.getMessage(), ex);
        }
    }

    private SecureFileUploadPolicy policyForModule(String module) {
        String normalizedModule = module == null ? "" : module.toLowerCase();
        if (normalizedModule.contains("photo")) {
            return SecureFileUploadPolicy.allowedExtensions(module, IMAGE_EXTENSIONS);
        }
        if (normalizedModule.contains("resume")
                || normalizedModule.contains("department-registration")
                || normalizedModule.contains("office-order")) {
            return SecureFileUploadPolicy.allowedExtensions(module, PDF_EXTENSIONS);
        }
        return SecureFileUploadPolicy.allowedExtensions(module, DEFAULT_DOCUMENT_EXTENSIONS);
    }

    private record StoredFileValidationKey(
            Path path,
            String module,
            long size,
            long lastModifiedMillis) {
    }
}
