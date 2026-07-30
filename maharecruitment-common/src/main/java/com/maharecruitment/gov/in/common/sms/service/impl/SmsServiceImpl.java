package com.maharecruitment.gov.in.common.sms.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.common.sms.client.SmsGatewayClient;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayRequest;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayResponse;
import com.maharecruitment.gov.in.common.sms.exception.SmsGatewayException;
import com.maharecruitment.gov.in.common.sms.service.SmsService;
import com.maharecruitment.gov.in.common.sms.service.SmsTransactionLogService;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateCode;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateDefinition;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateService;

@Service
public class SmsServiceImpl implements SmsService {

    private final SmsGatewayClient smsGatewayClient;
    private final SmsTemplateService templateService;
    private final SmsTransactionLogService logService;
    private final boolean smsEnabled;

    public SmsServiceImpl(
            SmsGatewayClient smsGatewayClient,
            SmsTemplateService templateService,
            SmsTransactionLogService logService,
            @Value("${app.service.sms-enabled:true}") boolean smsEnabled) {
        this.smsGatewayClient = smsGatewayClient;
        this.templateService = templateService;
        this.logService = logService;
        this.smsEnabled = smsEnabled;
    }

    @Override
    public void sendLoginOtp(Long userId, String mobileNumber, String otp) {
        SmsTemplateDefinition definition = templateService.getTemplateDefinition(SmsTemplateCode.LOGIN_OTP);
        sendResolvedTemplate(
                userId,
                mobileNumber,
                SmsTemplateCode.LOGIN_OTP,
                definition.appId(),
                templateService.buildLoginOtpMessage(otp));
    }

    @Override
    public void sendTemplateSms(
            Long userId,
            String mobileNumber,
            SmsTemplateCode templateCode,
            Map<String, String> parameters) {
        SmsTemplateDefinition definition = templateService.getTemplateDefinition(templateCode);
        sendResolvedTemplate(
                userId,
                mobileNumber,
                templateCode,
                definition.appId(),
                templateService.buildTemplateMessage(templateCode, parameters));
    }

    private void sendResolvedTemplate(
            Long userId,
            String mobileNumber,
            SmsTemplateCode templateCode,
            String appId,
            String message) {
        if (!smsEnabled) {
            throw new SmsGatewayException("SMS service is disabled");
        }

        String correlationId = UUID.randomUUID().toString();
        Long transactionId = logService.createPending(
                userId,
                mobileNumber,
                templateCode.name(),
                appId,
                correlationId);

        try {
            SmsGatewayResponse response = smsGatewayClient.send(
                    new SmsGatewayRequest(mobileNumber, message, correlationId, appId));
            logService.markSent(transactionId, response.providerResponse());
        } catch (RuntimeException exception) {
            logService.markFailed(transactionId, exception.getMessage());
            throw exception;
        }
    }
}
