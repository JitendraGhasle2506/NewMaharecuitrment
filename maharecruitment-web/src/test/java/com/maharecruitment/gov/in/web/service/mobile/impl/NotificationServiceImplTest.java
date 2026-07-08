package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeFcmToken;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeFcmTokenRepository;
import com.maharecruitment.gov.in.web.dto.mobile.FcmTokenRequest;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;

@ExtendWith(MockitoExtension.class)
class MobileNotificationServiceImplTest {

    @Mock
    private MobileEmployeeAccessService mobileEmployeeAccessService;

    @Mock
    private EmployeeFcmTokenRepository employeeFcmTokenRepository;

    @Test
    void saveTokenCreatesNewEmployeeDeviceToken() {
        when(employeeFcmTokenRepository.findByEmployeeIdAndDeviceId(20L, "device-1"))
                .thenReturn(Optional.empty());

        var response = service().saveToken(new FcmTokenRequest(
                20L,
                "AAAA123456789XYZ",
                "android",
                " device-1 "));

        ArgumentCaptor<EmployeeFcmToken> captor = ArgumentCaptor.forClass(EmployeeFcmToken.class);
        verify(employeeFcmTokenRepository).save(captor.capture());
        EmployeeFcmToken saved = captor.getValue();
        assertThat(saved.getEmployeeId()).isEqualTo(20L);
        assertThat(saved.getDeviceId()).isEqualTo("device-1");
        assertThat(saved.getFcmToken()).isEqualTo("AAAA123456789XYZ");
        assertThat(saved.getPlatform()).isEqualTo("ANDROID");
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("FCM token saved successfully");
        verify(mobileEmployeeAccessService).requireCurrentActiveEmployeeContext(20L);
    }

    @Test
    void saveTokenUpdatesExistingEmployeeDeviceToken() {
        EmployeeFcmToken existing = new EmployeeFcmToken();
        existing.setId(7L);
        existing.setEmployeeId(20L);
        existing.setDeviceId("device-1");
        existing.setFcmToken("old-token");
        existing.setPlatform("ANDROID");

        when(employeeFcmTokenRepository.findByEmployeeIdAndDeviceId(20L, "device-1"))
                .thenReturn(Optional.of(existing));

        var response = service().saveToken(new FcmTokenRequest(
                20L,
                "new-token",
                "IOS",
                "device-1"));

        verify(employeeFcmTokenRepository).save(existing);
        assertThat(existing.getFcmToken()).isEqualTo("new-token");
        assertThat(existing.getPlatform()).isEqualTo("IOS");
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("FCM token updated successfully");
    }

    @Test
    void saveTokenDoesNotPersistWhenEmployeeAccessFails() {
        when(mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(99L))
                .thenThrow(new MobileApiException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "EMPLOYEE_MISMATCH",
                        "You can access only the logged-in employee details."));

        assertThatThrownBy(() -> service().saveToken(new FcmTokenRequest(
                99L,
                "firebase-token",
                "ANDROID",
                "device-1")))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                    assertThat(ex.getCode()).isEqualTo("EMPLOYEE_MISMATCH");
                });

        verify(employeeFcmTokenRepository, never()).findByEmployeeIdAndDeviceId(any(), any());
        verify(employeeFcmTokenRepository, never()).save(any());
    }

    private MobileNotificationServiceImpl service() {
        return new MobileNotificationServiceImpl(mobileEmployeeAccessService, employeeFcmTokenRepository);
    }
}
