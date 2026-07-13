package com.maharecruitment.gov.in.common.sms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.common.sms.client.SmsGatewayClient;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayRequest;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayResponse;
import com.maharecruitment.gov.in.common.sms.service.SmsTransactionLogService;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateCode;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateProperties;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateService;

class SmsServiceImplTest {

    @Test
    void loginOtpUsesExactTemplateAndLoginAppId() {
        SmsGatewayClient gatewayClient = mock(SmsGatewayClient.class);
        SmsTransactionLogService logService = mock(SmsTransactionLogService.class);
        when(logService.createPending(any(), any(), any(), any(), any())).thenReturn(42L);
        when(gatewayClient.send(any())).thenReturn(new SmsGatewayResponse(true, "Message Submitted", "corr-1"));
        SmsServiceImpl service = new SmsServiceImpl(gatewayClient, templateService(), logService, true);

        service.sendLoginOtp(10L, "7020186501", "930712");

        ArgumentCaptor<SmsGatewayRequest> requestCaptor = ArgumentCaptor.forClass(SmsGatewayRequest.class);
        verify(gatewayClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().appId()).isEqualTo("LOGIN-APP-ID");
        assertThat(requestCaptor.getValue().message()).isEqualTo(
                "OTP for Maharecruitment Portal login: 930712. "
                        + "Valid for 10 minutes. "
                        + "Do not share this OTP with anyone. - MAHGOV");
    }

    @Test
    void genericSmsUsesSelectedTemplateAppId() {
        SmsGatewayClient gatewayClient = mock(SmsGatewayClient.class);
        SmsTransactionLogService logService = mock(SmsTransactionLogService.class);
        when(logService.createPending(any(), any(), any(), any(), any())).thenReturn(43L);
        when(gatewayClient.send(any())).thenReturn(new SmsGatewayResponse(true, "Message Submitted", "corr-2"));
        SmsServiceImpl service = new SmsServiceImpl(gatewayClient, templateService(), logService, true);

        service.sendTemplateSms(
                10L,
                "7020186501",
                SmsTemplateCode.APPLICATION_SUBMITTED,
                Map.of("application_id", "A-100"));

        ArgumentCaptor<SmsGatewayRequest> requestCaptor = ArgumentCaptor.forClass(SmsGatewayRequest.class);
        verify(gatewayClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().appId()).isEqualTo("SUBMITTED-APP-ID");
        assertThat(requestCaptor.getValue().message()).isEqualTo("Application A-100 submitted.");
    }

    private SmsTemplateService templateService() {
        SmsTemplateProperties properties = new SmsTemplateProperties();
        SmsTemplateProperties.Template login = new SmsTemplateProperties.Template();
        login.setAppId("LOGIN-APP-ID");
        login.setMessage("OTP for Maharecruitment Portal login: {otp}. Valid for 10 minutes. Do not share this OTP with anyone. - MAHGOV");
        login.setRequiredParameters(Set.of("otp"));

        SmsTemplateProperties.Template applicationSubmitted = new SmsTemplateProperties.Template();
        applicationSubmitted.setAppId("SUBMITTED-APP-ID");
        applicationSubmitted.setMessage("Application {application_id} submitted.");
        applicationSubmitted.setRequiredParameters(Set.of("application_id"));

        properties.setTemplates(new LinkedHashMap<>(Map.of(
                "login-otp", login,
                "application-submitted", applicationSubmitted)));
        return new SmsTemplateService(properties);
    }
}
