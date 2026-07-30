package com.maharecruitment.gov.in.web.controller.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardControllerPhotoTest {

    @Mock
    private EmployeeProfileService employeeProfileService;

    @Test
    void photoEndpointReturnsNotFoundWhenNoValidEmployeePhotoCanBeResolved() {
        Principal principal = () -> "employee@example.gov.in";
        when(employeeProfileService.resolveCurrentEmployeePhoto(principal.getName()))
                .thenReturn(Optional.empty());

        EmployeeDashboardController controller =
                new EmployeeDashboardController(employeeProfileService);

        assertThat(controller.photo(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.photo(principal).getBody()).isNull();
    }
}
