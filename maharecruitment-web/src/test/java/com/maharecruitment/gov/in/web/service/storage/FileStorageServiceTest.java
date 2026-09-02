package com.maharecruitment.gov.in.web.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.maharecruitment.gov.in.common.upload.SecureFileUploadService;
import com.maharecruitment.gov.in.common.upload.SecureFileUploadPolicy;
import com.maharecruitment.gov.in.web.properties.FileUploadProperties;

class FileStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void resolveManagedPathAcceptsRelativePathInsideUploadBase() throws Exception {
        Path photoPath = tempDir.resolve("employee-photo").resolve("2026").resolve("07").resolve("photo.jpg");
        Files.createDirectories(photoPath.getParent());
        Files.write(photoPath, new byte[] { 1, 2, 3 });

        FileUploadProperties properties = new FileUploadProperties();
        properties.setBasePath(tempDir.toString());
        FileStorageService service = new FileStorageService(properties, mock(SecureFileUploadService.class));

        assertThat(service.resolveManagedPath("employee-photo/2026/07/photo.jpg"))
                .contains(photoPath.toAbsolutePath().normalize());
    }

    @Test
    void resolveManagedPathRejectsRelativeTraversalOutsideUploadBase() throws Exception {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setBasePath(tempDir.toString());
        FileStorageService service = new FileStorageService(properties, mock(SecureFileUploadService.class));

        assertThat(service.resolveManagedPath("../photo.jpg")).isEmpty();
    }

    @Test
    void reusesSecurityValidationForAnUnchangedManagedFile() throws Exception {
        Path photoPath = tempDir.resolve("employee-profile-photo").resolve("photo.jpg");
        Files.createDirectories(photoPath.getParent());
        Files.write(photoPath, new byte[] { 1, 2, 3 });

        FileUploadProperties properties = new FileUploadProperties();
        properties.setBasePath(tempDir.toString());
        SecureFileUploadService secureFileUploadService = mock(SecureFileUploadService.class);
        when(secureFileUploadService.isStoredFileAllowed(
                eq(photoPath.toAbsolutePath().normalize()),
                any(SecureFileUploadPolicy.class))).thenReturn(true);
        FileStorageService service = new FileStorageService(properties, secureFileUploadService);

        assertThat(service.isManagedFileAllowed(
                "employee-profile-photo/photo.jpg", "employee-profile-photo")).isTrue();
        assertThat(service.isManagedFileAllowed(
                "employee-profile-photo/photo.jpg", "employee-profile-photo")).isTrue();

        verify(secureFileUploadService, times(1)).isStoredFileAllowed(
                eq(photoPath.toAbsolutePath().normalize()),
                any(SecureFileUploadPolicy.class));
    }
}
