package com.maharecruitment.gov.in.web.controller.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileUpdateResponse;
import com.maharecruitment.gov.in.web.service.employee.EmployeeDashboardContentService;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardControllerPhotoTest {

    @TempDir
    private Path tempDir;

    @Mock
    private EmployeeProfileService employeeProfileService;

    @Mock
    private EmployeeDashboardContentService employeeDashboardContentService;

    @Test
    void photoEndpointReturnsNotFoundWhenNoValidEmployeePhotoCanBeResolved() {
        Principal principal = () -> "employee@example.gov.in";
        when(employeeProfileService.resolveCurrentEmployeePhoto(principal.getName()))
                .thenReturn(Optional.empty());

        EmployeeDashboardController controller =
                new EmployeeDashboardController(employeeProfileService, employeeDashboardContentService);

        assertThat(controller.photo(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.photo(principal).getBody()).isNull();
    }

    @Test
    void photoEndpointAllowsPrivateBrowserCaching() throws Exception {
        Principal principal = () -> "employee@example.gov.in";
        Path photo = tempDir.resolve("profile.jpg");
        Files.write(photo, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        when(employeeProfileService.resolveCurrentEmployeePhoto(principal.getName()))
                .thenReturn(Optional.of(photo));
        EmployeeDashboardController controller =
                new EmployeeDashboardController(employeeProfileService, employeeDashboardContentService);

        var response = controller.photo(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl())
                .contains("max-age=86400")
                .contains("private");
    }

    @Test
    void uploadEndpointReturnsJsonErrorWhenUnexpectedFailureOccurs() {
        Principal principal = () -> "employee@example.gov.in";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        when(employeeProfileService.uploadCurrentEmployeePhoto(eq(principal.getName()), any()))
                .thenThrow(new IllegalStateException("Storage unavailable"));
        EmployeeDashboardController controller =
                new EmployeeDashboardController(employeeProfileService, employeeDashboardContentService);

        var response = controller.uploadPhoto(principal, null, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .extracting(EmployeeProfileUpdateResponse::success, EmployeeProfileUpdateResponse::message)
                .containsExactly(false, "Unable to upload photo. Please try again after some time.");
    }
}
