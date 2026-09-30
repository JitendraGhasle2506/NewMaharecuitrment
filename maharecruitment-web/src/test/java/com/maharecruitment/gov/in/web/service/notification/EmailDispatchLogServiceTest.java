package com.maharecruitment.gov.in.web.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.web.entity.notification.EmailDispatchLogEntity;
import com.maharecruitment.gov.in.web.repository.notification.EmailDispatchLogRepository;

class EmailDispatchLogServiceTest {

    @Test
    void createsOnePendingAuditRecordPerDistinctRecipientWithoutMessageBody() {
        EmailDispatchLogRepository repository = mock(EmailDispatchLogRepository.class);
        when(repository.saveAll(org.mockito.ArgumentMatchers.<List<EmailDispatchLogEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        EmailDispatchLogService service = new EmailDispatchLogService(repository);

        service.createPending(
                "noreply@example.com",
                new String[] {"first@example.com", "second@example.com", "first@example.com"},
                "Password changed");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmailDispatchLogEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2).allSatisfy(log -> {
            assertThat(log.getSenderEmail()).isEqualTo("noreply@example.com");
            assertThat(log.getReason()).isEqualTo("Password changed");
            assertThat(log.getStatus()).isEqualTo("PENDING");
            assertThat(log.getRequestedAt()).isNotNull();
            assertThat(log.getFailureReason()).isNull();
        });
        assertThat(captor.getValue())
                .extracting(EmailDispatchLogEntity::getRecipientEmail)
                .containsExactly("first@example.com", "second@example.com");
    }
}
