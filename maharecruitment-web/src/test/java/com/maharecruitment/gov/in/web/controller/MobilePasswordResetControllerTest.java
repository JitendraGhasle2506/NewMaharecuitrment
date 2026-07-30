package com.maharecruitment.gov.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetResponse;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetService;
import com.maharecruitment.gov.in.web.service.passwordreset.ResetPasswordChannel;

@ExtendWith(MockitoExtension.class)
class MobilePasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @Test
    void requestOtpDelegatesWithMobileApiChannel() {
        MobilePasswordResetController controller = new MobilePasswordResetController(
                passwordResetService,
                new TransportSecurityProperties());
        PasswordResetOtpRequest request = new PasswordResetOtpRequest();
        request.setIdentifier("EMP000033");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("10.0.0.5");
        httpRequest.addHeader("User-Agent", "JUnit");
        PasswordResetResponse response = PasswordResetResponse.accepted("accepted");
        when(passwordResetService.requestOtp(
                eq(request),
                eq(ResetPasswordChannel.MOBILE_API),
                eq("10.0.0.5"),
                eq("JUnit"))).thenReturn(response);

        ResponseEntity<PasswordResetResponse> result = controller.requestOtp(request, httpRequest);

        assertThat(result.getBody()).isSameAs(response);
        verify(passwordResetService).requestOtp(
                request,
                ResetPasswordChannel.MOBILE_API,
                "10.0.0.5",
                "JUnit");
    }
}
