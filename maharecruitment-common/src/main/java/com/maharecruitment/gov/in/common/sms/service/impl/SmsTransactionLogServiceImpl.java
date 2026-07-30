package com.maharecruitment.gov.in.common.sms.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.common.sms.entity.SmsTransactionLogEntity;
import com.maharecruitment.gov.in.common.sms.repository.SmsTransactionLogRepository;
import com.maharecruitment.gov.in.common.sms.service.SmsTransactionLogService;
import com.maharecruitment.gov.in.common.sms.util.MobileNumberUtil;

@Service
public class SmsTransactionLogServiceImpl implements SmsTransactionLogService {

    private static final int PROVIDER_RESPONSE_MAX_LENGTH = 1000;

    private final SmsTransactionLogRepository repository;

    public SmsTransactionLogServiceImpl(SmsTransactionLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createPending(
            Long userId,
            String mobileNumber,
            String smsType,
            String templateId,
            String correlationId) {
        SmsTransactionLogEntity entity = new SmsTransactionLogEntity();
        entity.setUserId(userId);
        entity.setMobileNumberMasked(MobileNumberUtil.mask(mobileNumber));
        entity.setSmsType(smsType);
        entity.setTemplateId(templateId);
        entity.setCorrelationId(correlationId);
        entity.setStatus("PENDING");
        return repository.save(entity).getSmsTransactionId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long transactionId, String providerResponse) {
        repository.findById(transactionId).ifPresent(entity -> {
            entity.setStatus("SENT");
            entity.setSentOn(Instant.now());
            entity.setProviderResponse(truncate(providerResponse));
            repository.save(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long transactionId, String failureReason) {
        repository.findById(transactionId).ifPresent(entity -> {
            entity.setStatus("FAILED");
            entity.setFailedOn(Instant.now());
            entity.setProviderResponse(truncate(failureReason));
            repository.save(entity);
        });
    }

    private String truncate(String value) {
        if (value == null || value.length() <= PROVIDER_RESPONSE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, PROVIDER_RESPONSE_MAX_LENGTH);
    }
}
