package com.maharecruitment.gov.in.common.sms.dto;

public record SmsGatewayRequest(
        String mobileNumber,
        String message,
        String correlationId,
        String appId
) {

    public SmsGatewayRequest(String mobileNumber, String message, String correlationId) {
        this(mobileNumber, message, correlationId, null);
    }
}
