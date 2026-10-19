package com.maharecruitment.gov.in.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.entity.AuditEventEntity;
import com.maharecruitment.gov.in.audit.repository.AuditEventRepository;
import com.maharecruitment.gov.in.auth.service.UserService;
import com.maharecruitment.gov.in.common.audit.AuditRequestContext;

class AuditTrailServiceImplTest {
    @Test void storesCanonicalIpForEveryActionAndReturnsItInTheTimeline() {
        var repository = mock(AuditEventRepository.class);
        var service = new AuditTrailServiceImpl(repository, mock(UserService.class));
        try (var context = AuditRequestContext.open("2001:db8::7")) {
            service.record(AuditRecordRequest.builder().moduleName("TEST").entityType("EMPLOYEE")
                    .entityId("4").actionType("UPDATE").activitySummary("Changed assignment")
                    .ipAddress("198.51.100.1").metadata(Map.of("clientIp", "forged", "projectId", 3L)).build());
        }
        var captured = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(repository).save(captured.capture());
        var event = captured.getValue();
        assertThat(event.getIpAddress()).isEqualTo("2001:db8::7");
        assertThat(event.getMetadataJson()).contains("2001:db8::7", "projectId").doesNotContain("forged");
        when(repository.findByModuleNameAndEntityTypeAndEntityIdOrderByOccurredAtDescAuditEventIdDesc("TEST", "EMPLOYEE", "4"))
                .thenReturn(List.of(event));
        assertThat(service.getTimeline("TEST", "EMPLOYEE", "4").getFirst().getIpAddress()).isEqualTo("2001:db8::7");
    }
}
