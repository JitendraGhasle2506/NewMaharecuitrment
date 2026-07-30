package com.maharecruitment.gov.in.common.sms.service;

public interface SmsTransactionLogService {

    Long createPending(
            Long userId,
            String mobileNumber,
            String smsType,
            String templateId,
            String correlationId);

    void markSent(Long transactionId, String providerResponse);

    void markFailed(Long transactionId, String failureReason);
}
