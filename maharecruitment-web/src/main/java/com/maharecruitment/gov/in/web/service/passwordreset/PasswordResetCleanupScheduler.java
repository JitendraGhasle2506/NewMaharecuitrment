package com.maharecruitment.gov.in.web.service.passwordreset;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetCleanupScheduler {

    private final PasswordResetService passwordResetService;

    public PasswordResetCleanupScheduler(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Scheduled(cron = "${security.password-reset.cleanup-cron:0 */15 * * * *}")
    public void expirePasswordResetRequests() {
        passwordResetService.invalidateExpiredRequests();
    }
}
