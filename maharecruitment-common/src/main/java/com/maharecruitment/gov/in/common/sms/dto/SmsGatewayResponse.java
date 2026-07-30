package com.maharecruitment.gov.in.common.sms.dto;

public record SmsGatewayResponse(
        boolean accepted,
        String providerResponse,
        String correlationId
) {
}
