package com.maharecruitment.gov.in.common.sms.client;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.maharecruitment.gov.in.common.sms.config.AclSmsProperties;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayRequest;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayResponse;
import com.maharecruitment.gov.in.common.sms.exception.SmsGatewayException;
import com.maharecruitment.gov.in.common.sms.util.MobileNumberUtil;

@Component
public class AclSmsGatewayClient implements SmsGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AclSmsGatewayClient.class);

    private final RestClient restClient;
    private final AclSmsProperties properties;

    public AclSmsGatewayClient(
            @Qualifier("aclSmsRestClient") RestClient aclSmsRestClient,
            AclSmsProperties properties) {
        this.restClient = aclSmsRestClient;
        this.properties = properties;
    }

    @Override
    public SmsGatewayResponse send(SmsGatewayRequest request) {
        validateEnabledConfiguration();

        String mobileNumber = MobileNumberUtil.normalizeIndianMobileNumber(request.mobileNumber());
        String appId = StringUtils.hasText(request.appId()) ? request.appId().trim() : properties.appId();
        if (!StringUtils.hasText(appId)) {
            throw new SmsGatewayException("SMS template app ID is not configured");
        }

        URI requestUri = UriComponentsBuilder
                .fromUriString(properties.baseUrl())
                .queryParam("contenttype", properties.contentType())
                .queryParam("from", properties.senderId())
                .queryParam("alert", properties.alert())
                .queryParam("selfid", properties.selfId())
                .queryParam("appid", appId)
                .queryParam("userId", properties.userId())
                .queryParam("pass", properties.password())
                .queryParam("to", mobileNumber)
                .queryParam("text", request.message())
                .build()
                .encode()
                .toUri();

        try {
            String providerResponse = restClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .body(String.class);

            boolean accepted = isAcceptedResponse(providerResponse);
            log.info(
                    "ACL SMS request completed. correlationId={}, mobile={}, accepted={}",
                    request.correlationId(),
                    MobileNumberUtil.mask(mobileNumber),
                    accepted);

            if (!accepted) {
                throw new SmsGatewayException("SMS provider rejected the request");
            }

            return new SmsGatewayResponse(true, providerResponse, request.correlationId());
        } catch (RestClientException exception) {
            log.error(
                    "ACL SMS gateway request failed. correlationId={}, mobile={}, errorType={}",
                    request.correlationId(),
                    MobileNumberUtil.mask(mobileNumber),
                    exception.getClass().getSimpleName());
            throw new SmsGatewayException("Unable to submit SMS to gateway", exception);
        }
    }

    boolean isAcceptedResponse(String providerResponse) {
        if (providerResponse == null || providerResponse.isBlank()) {
            return false;
        }

        String normalized = providerResponse.trim().toLowerCase();
        return !normalized.contains("error")
                && !normalized.contains("failed")
                && !normalized.contains("invalid");
    }

    private void validateEnabledConfiguration() {
        requireConfigured(properties.baseUrl(), "SMS gateway base URL is not configured");
        requireConfigured(properties.senderId(), "SMS sender ID is not configured");
        requireConfigured(properties.userId(), "SMS gateway user ID is not configured");
        requireConfigured(properties.password(), "SMS gateway password is not configured");
    }

    private void requireConfigured(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new SmsGatewayException(message);
        }
    }
}
