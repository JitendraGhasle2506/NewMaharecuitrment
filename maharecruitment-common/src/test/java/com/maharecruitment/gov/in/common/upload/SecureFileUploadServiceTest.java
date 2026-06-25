package com.maharecruitment.gov.in.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class SecureFileUploadServiceTest {

    @TempDir
    private Path tempDir;

    private SecureFileUploadService secureFileUploadService;
    private SecureFileUploadPolicy defaultPolicy;

    @BeforeEach
    void setUp() {
        SecureFileUploadProperties properties = new SecureFileUploadProperties();
        secureFileUploadService = new SecureFileUploadService(properties);
        defaultPolicy = SecureFileUploadPolicy.allowedExtensions("test-upload", Set.of("pdf", "jpg", "jpeg", "png"));
    }

    @Test
    void validPdfUploadShouldPass() {
        ValidatedFileUpload result = secureFileUploadService.validate(
                file("document.pdf", "application/pdf", pdfBytes()),
                defaultPolicy);

        assertThat(result.extension()).isEqualTo("pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.storedFileName()).endsWith(".pdf");
        assertThat(result.storedFileName()).doesNotContain("document");
    }

    @Test
    void validJpgUploadShouldPass() {
        ValidatedFileUpload result = secureFileUploadService.validate(
                file("photo.JPG", "image/jpeg", jpegBytes()),
                defaultPolicy);

        assertThat(result.extension()).isEqualTo("jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void validPngUploadShouldPass() {
        ValidatedFileUpload result = secureFileUploadService.validate(
                file("image.png", "image/png", pngBytes()),
                defaultPolicy);

        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void dangerousExtensionsShouldFail() {
        for (String extension : List.of("jsp", "exe", "php")) {
            assertThatThrownBy(() -> secureFileUploadService.validate(
                    file("payload." + extension, "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8)),
                    defaultPolicy))
                    .isInstanceOf(SecureFileUploadException.class);
        }
    }

    @Test
    void doubleExtensionShouldFail() {
        for (String fileName : List.of("invoice.pdf.exe", "payload.jsp.pdf", "test.php.jpg")) {
            assertThatThrownBy(() -> secureFileUploadService.validate(
                    file(fileName, "application/pdf", pdfBytes()),
                    defaultPolicy))
                    .isInstanceOf(SecureFileUploadException.class);
        }
    }

    @Test
    void invalidMimeShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("document.pdf", "text/plain", pdfBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void executableRenamedAsPdfShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("document.pdf", "application/pdf", mzBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void oversizedFileShouldFail() {
        byte[] oversizedPdf = new byte[(int) DataSize.ofMegabytes(2).toBytes() + 1];
        byte[] signature = pdfBytes();
        System.arraycopy(signature, 0, oversizedPdf, 0, signature.length);

        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("large.pdf", "application/pdf", oversizedPdf),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void emptyFileShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("empty.pdf", "application/pdf", new byte[0]),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void pathTraversalFileNameShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("../document.pdf", "application/pdf", pdfBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void nullByteFileNameShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("document%00.pdf", "application/pdf", pdfBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void veryLongFileNameShouldFail() {
        String longFileName = "a".repeat(97) + ".pdf";

        assertThatThrownBy(() -> secureFileUploadService.validate(
                file(longFileName, "application/pdf", pdfBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void specialCharacterFileNameShouldFail() {
        assertThatThrownBy(() -> secureFileUploadService.validate(
                file("bad&file.pdf", "application/pdf", pdfBytes()),
                defaultPolicy))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void secureDirectoryShouldStayInsideBaseDirectory() {
        Path resolved = secureFileUploadService.resolveSecureDirectory(tempDir, "department-registration", "gst");

        assertThat(resolved.startsWith(tempDir.toAbsolutePath().normalize())).isTrue();
        assertThat(resolved).isEqualTo(tempDir.resolve("department-registration").resolve("gst")
                .toAbsolutePath()
                .normalize());
    }

    @Test
    void secureDirectoryShouldRejectPathTraversal() {
        assertThatThrownBy(() -> secureFileUploadService.resolveSecureDirectory(
                tempDir,
                "department-registration",
                "..",
                "..",
                "outside"))
                .isInstanceOf(SecureFileUploadException.class);
    }

    @Test
    void secureFileShouldStayInsideUploadDirectory() {
        Path uploadDirectory = tempDir.resolve("safe-upload").toAbsolutePath().normalize();
        String storedFileName = "7b5e5c23-9a6a-48da-bba8-f91d7f74d33f.pdf";

        Path resolved = secureFileUploadService.resolveSecureFile(uploadDirectory, storedFileName);

        assertThat(resolved.startsWith(uploadDirectory)).isTrue();
        assertThat(resolved.getFileName().toString()).isEqualTo(storedFileName);
    }

    @Test
    void secureFileShouldRejectStoredFilePathTraversal() {
        Path uploadDirectory = tempDir.resolve("safe-upload").toAbsolutePath().normalize();

        for (String storedFileName : List.of("../evil.pdf", "..\\evil.pdf", "nested/evil.pdf")) {
            assertThatThrownBy(() -> secureFileUploadService.resolveSecureFile(uploadDirectory, storedFileName))
                    .isInstanceOf(SecureFileUploadException.class);
        }
    }

    private MockMultipartFile file(String fileName, String contentType, byte[] content) {
        return new MockMultipartFile("file", fileName, contentType, content);
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\n%secure-test".getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'
        };
    }

    private byte[] pngBytes() {
        byte[] data = Arrays.copyOf(new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        }, 12);
        data[8] = 0x00;
        data[9] = 0x00;
        data[10] = 0x00;
        data[11] = 0x0D;
        return data;
    }

    private byte[] mzBytes() {
        return new byte[] { 'M', 'Z', 0x00, 0x00, 0x00 };
    }
}
