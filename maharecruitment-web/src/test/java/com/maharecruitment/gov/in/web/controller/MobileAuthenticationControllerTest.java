package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticationService;

@ExtendWith(MockitoExtension.class)
class MobileAuthenticationControllerTest {

    @Mock
    private MobileAuthenticationService mobileAuthenticationService;

    @Test
    void logoutDelegatesToAuthenticationService() {
        MobileAuthenticationController controller = new MobileAuthenticationController(mobileAuthenticationService);
        MobileLogoutRequest request = new MobileLogoutRequest("refresh-token", true);
        MobileLogoutResponse response = new MobileLogoutResponse(true, "Logged out successfully.");
        when(mobileAuthenticationService.logout(request)).thenReturn(response);

        ResponseEntity<MobileLogoutResponse> result = controller.logout(request);

        assertThat(result.getBody()).isSameAs(response);
        verify(mobileAuthenticationService).logout(request);
    }
}
