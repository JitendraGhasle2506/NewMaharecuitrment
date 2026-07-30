package com.maharecruitment.gov.in.common.sms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.common.sms.entity.SmsTransactionLogEntity;
import com.maharecruitment.gov.in.common.sms.repository.SmsTransactionLogRepository;

class SmsTransactionLogServiceImplTest {

    @Test
    void createPendingStoresMaskedMobileOnly() {
        SmsTransactionLogRepository repository = mock(SmsTransactionLogRepository.class);
        when(repository.save(any(SmsTransactionLogEntity.class))).thenAnswer(invocation -> {
            SmsTransactionLogEntity entity = invocation.getArgument(0);
            entity.setSmsTransactionId(42L);
            return entity;
        });
        SmsTransactionLogServiceImpl service = new SmsTransactionLogServiceImpl(repository);

        Long transactionId = service.createPending(
                10L,
                "+91 7020186501",
                "LOGIN_OTP",
                "1707178340749813454",
                "corr-1");

        ArgumentCaptor<SmsTransactionLogEntity> captor = ArgumentCaptor.forClass(SmsTransactionLogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(transactionId).isEqualTo(42L);
        assertThat(captor.getValue().getMobileNumberMasked()).isEqualTo("******6501");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void markSentAndFailedUpdateStatus() {
        SmsTransactionLogRepository repository = mock(SmsTransactionLogRepository.class);
        SmsTransactionLogEntity entity = new SmsTransactionLogEntity();
        entity.setSmsTransactionId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(repository.save(any(SmsTransactionLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SmsTransactionLogServiceImpl service = new SmsTransactionLogServiceImpl(repository);

        service.markSent(42L, "Message Submitted");
        assertThat(entity.getStatus()).isEqualTo("SENT");
        assertThat(entity.getProviderResponse()).isEqualTo("Message Submitted");

        service.markFailed(42L, "Unable to submit SMS to gateway");
        assertThat(entity.getStatus()).isEqualTo("FAILED");
        assertThat(entity.getProviderResponse()).isEqualTo("Unable to submit SMS to gateway");
    }
}
