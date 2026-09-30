package com.maharecruitment.gov.in.web.service.notification;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.entity.notification.EmailDispatchLogEntity;
import com.maharecruitment.gov.in.web.repository.notification.EmailDispatchLogRepository;

@Service
public class EmailDispatchLogService {

    private static final int FAILURE_REASON_MAX_LENGTH = 1000;
    private static final int SUBJECT_MAX_LENGTH = 500;

    private final EmailDispatchLogRepository repository;

    public EmailDispatchLogService(EmailDispatchLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> createPending(String sender, String[] recipients, String subject) {
        if (recipients == null) {
            return List.of();
        }

        List<EmailDispatchLogEntity> logs = Arrays.stream(recipients)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(recipient -> pendingLog(sender, recipient, subject))
                .toList();
        if (logs.isEmpty()) {
            return List.of();
        }
        return repository.saveAll(logs).stream().map(EmailDispatchLogEntity::getId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Collection<Long> logIds) {
        updateStatus(logIds, "SENT", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Collection<Long> logIds, Throwable failure) {
        updateStatus(logIds, "FAILED", failureSummary(failure));
    }

    private EmailDispatchLogEntity pendingLog(String sender, String recipient, String subject) {
        EmailDispatchLogEntity log = new EmailDispatchLogEntity();
        log.setSenderEmail(trimToNull(sender));
        log.setRecipientEmail(recipient);
        log.setReason(truncate(StringUtils.hasText(subject) ? subject.trim() : "Unspecified application email", SUBJECT_MAX_LENGTH));
        log.setStatus("PENDING");
        log.setRequestedAt(Instant.now());
        return log;
    }

    private void updateStatus(Collection<Long> logIds, String status, String failureReason) {
        if (logIds == null || logIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        List<EmailDispatchLogEntity> logs = repository.findAllById(logIds);
        logs.forEach(log -> {
            log.setStatus(status);
            if ("SENT".equals(status)) {
                log.setSentAt(now);
            } else {
                log.setFailedAt(now);
                log.setFailureReason(failureReason);
            }
        });
        repository.saveAll(logs);
    }

    private String failureSummary(Throwable failure) {
        if (failure == null) {
            return "Unknown email delivery failure";
        }
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = StringUtils.hasText(root.getMessage()) ? root.getMessage().trim() : "No details available";
        return truncate(root.getClass().getSimpleName() + ": " + message, FAILURE_REASON_MAX_LENGTH);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int maxLength) {
        return Objects.requireNonNull(value).length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
