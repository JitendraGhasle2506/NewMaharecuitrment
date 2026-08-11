package com.maharecruitment.gov.in.web.service.verification.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.entity.verification.OtpVerificationStateEntity;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.repository.verification.OtpVerificationStateRepository;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryReferences;
import com.maharecruitment.gov.in.web.service.verification.OtpFailureReason;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimiter;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpSecurityAuditService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

class DatabaseOtpVerificationServiceTest {

    private static final String PURPOSE = VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT;
    private static final String EMAIL = "user@example.com";
    private static final OtpRequestContext CONTEXT = new OtpRequestContext("127.0.0.1");

    private final Map<String, OtpVerificationStateEntity> stateStore = new ConcurrentHashMap<>();

    private CapturingEmailHandler emailHandler;
    private CapturingBothHandler bothHandler;
    private OtpVerificationProperties properties;
    private OtpVerificationStateRepository repository;
    private DatabaseOtpVerificationService service;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        properties = new OtpVerificationProperties();
        properties.setExpiryMinutes(5);
        properties.setMaxAttempts(5);
        properties.setLockDurationMinutes(15);
        properties.setResendLimit(3);
        properties.setResendWindowMinutes(5);
        properties.setVerifyRateLimit(10);
        properties.setVerifyRateWindowSeconds(60);
        properties.setCaptchaThreshold(3);
        properties.setOtpLength(6);
        properties.setResendCooldownSeconds(60);

        repository = mock(OtpVerificationStateRepository.class);
        when(repository.findBySessionIdAndPurposeAndChannel(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(stateStore.get(key(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)))));
        when(repository.save(any(OtpVerificationStateEntity.class))).thenAnswer(invocation -> {
            OtpVerificationStateEntity state = invocation.getArgument(0);
            if (state.getOtpStateId() == null) {
                state.setOtpStateId((long) stateStore.size() + 1L);
            }
            stateStore.put(key(state.getSessionId(), state.getPurpose(), state.getChannel()), state);
            return state;
        });
        doAnswer(invocation -> {
            String sessionId = invocation.getArgument(0);
            String purpose = invocation.getArgument(1);
            stateStore.keySet().removeIf(key -> key.startsWith(sessionId + ":" + purpose + ":"));
            return null;
        }).when(repository).deleteBySessionIdAndPurpose(any(), any());
        doAnswer(invocation -> {
            stateStore.remove(key(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
            return null;
        }).when(repository).deleteBySessionIdAndPurposeAndChannel(any(), any(), any());

        emailHandler = new CapturingEmailHandler();
        bothHandler = new CapturingBothHandler();
        service = new DatabaseOtpVerificationService(
                List.of(emailHandler, bothHandler),
                properties,
                repository,
                new OtpRateLimiter(properties),
                mock(OtpSecurityAuditService.class));
        session = new MockHttpSession();
    }

    @Test
    void validOtpSucceeds() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);

        OtpVerificationResult result = service.verifyOtp(
                session,
                PURPOSE,
                VerificationChannel.EMAIL,
                EMAIL,
                emailHandler.lastOtp,
                null,
                null,
                CONTEXT);

        assertTrue(result.verified());
        assertTrue(service.isVerified(session, PURPOSE, VerificationChannel.EMAIL, EMAIL));
        assertEquals(0, currentState().getOtpAttemptCount());
        assertFalse(currentState().isOtpVerified() && currentState().getOtpHash() != null);
    }

    @Test
    void sentOtpIncludesUserFacingOtpReferenceId() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);

        assertNotNull(emailHandler.lastOtpReferenceId);
        assertTrue(emailHandler.lastOtpReferenceId.matches("OTP-[A-Z2-9]{6}"));
    }

    @Test
    void wrongOtpIncrementsCounter() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);

        OtpVerificationException exception = assertThrows(
                OtpVerificationException.class,
                () -> verify("000000"));

        assertEquals(OtpFailureReason.INVALID_OTP, exception.getReason());
        assertEquals(1, currentState().getOtpAttemptCount());
        assertEquals(4, exception.getResult().remainingAttempts());
    }

    @Test
    void afterFiveWrongAttemptsOtpIsLockedAndInvalidated() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);

        OtpVerificationException exception = null;
        for (int i = 0; i < 5; i++) {
            exception = assertThrows(OtpVerificationException.class, () -> verifyWrongOtpWithCaptchaWhenRequired());
        }

        assertNotNull(exception);
        assertEquals(OtpFailureReason.LOCKED, exception.getReason());
        assertEquals(0, exception.getResult().remainingAttempts());
        assertNotNull(currentState().getOtpLockedUntil());
        assertFalse(currentState().isOtpVerified());
        assertNull(currentState().getOtpHash());
    }

    @Test
    void correctOtpAfterFiveWrongAttemptsFails() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String correctOtp = emailHandler.lastOtp;

        for (int i = 0; i < 5; i++) {
            assertThrows(OtpVerificationException.class, () -> verifyWrongOtpWithCaptchaWhenRequired());
        }

        OtpVerificationException exception = assertThrows(
                OtpVerificationException.class,
                () -> verify(correctOtp));

        assertEquals(OtpFailureReason.LOCKED, exception.getReason());
        assertFalse(service.isVerified(session, PURPOSE, VerificationChannel.EMAIL, EMAIL));
    }

    @Test
    void newOtpAfterResendInvalidatesOldOtp() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String oldOtp = emailHandler.lastOtp;
        currentState().setOtpExpiryTime(Instant.now().minusSeconds(1));
        properties.setResendCooldownSeconds(0);

        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String newOtp = emailHandler.lastOtp;

        assertThrows(OtpVerificationException.class, () -> verify(oldOtp));

        OtpVerificationResult result = verify(newOtp);
        assertTrue(result.verified());
    }

    @Test
    void resendIsBlockedWhileCurrentOtpIsStillValid() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String activeOtp = emailHandler.lastOtp;

        OtpVerificationException exception = assertThrows(
                OtpVerificationException.class,
                () -> service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT));

        assertEquals(OtpFailureReason.RATE_LIMITED, exception.getReason());
        assertTrue(exception.getResult().retryAfterSeconds() > 0);
        assertEquals(activeOtp, emailHandler.lastOtp);
    }

    @Test
    void smsOnlyDispatchFailureDoesNotPersistOtpHash() {
        DatabaseOtpVerificationService failingSmsService = new DatabaseOtpVerificationService(
                List.of(new FailingSmsHandler()),
                properties,
                repository,
                new OtpRateLimiter(properties),
                mock(OtpSecurityAuditService.class));

        assertThrows(IllegalStateException.class, () -> failingSmsService.sendOtp(
                session,
                PURPOSE,
                VerificationChannel.SMS,
                "7020186501",
                CONTEXT));

        assertNull(stateStore.get(key(session.getId(), PURPOSE, VerificationChannel.SMS.name())));
    }

    @Test
    void dispatchFailureDoesNotConsumeSendRateLimit() {
        properties.setResendLimit(1);
        properties.setSendIpLimit(1);
        DatabaseOtpVerificationService failingService = new DatabaseOtpVerificationService(
                List.of(new FailingSmsHandler()),
                properties,
                repository,
                new OtpRateLimiter(properties),
                mock(OtpSecurityAuditService.class));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThrows(IllegalStateException.class, () -> failingService.sendOtp(
                    session,
                    PURPOSE,
                    VerificationChannel.SMS,
                    "7020186501",
                    CONTEXT));
        }
    }

    @Test
    void bothChannelDispatchesSameOtpToEmailAndSms() {
        service.sendOtp(
                session,
                VerificationPurposes.LOGIN_AUTHENTICATION,
                VerificationChannel.BOTH,
                OtpDeliveryReferences.both("user@example.com", "7020186501"),
                CONTEXT);

        assertNotNull(bothHandler.emailOtp);
        assertEquals(bothHandler.emailOtp, bothHandler.smsOtp);
        assertEquals(VerificationChannel.BOTH.name(), stateStore
                .get(key(session.getId(), VerificationPurposes.LOGIN_AUTHENTICATION, VerificationChannel.BOTH.name()))
                .getChannel());
    }

    @Test
    void expiredOtpFails() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String otp = emailHandler.lastOtp;
        currentState().setOtpExpiryTime(Instant.now().minusSeconds(1));

        OtpVerificationException exception = assertThrows(
                OtpVerificationException.class,
                () -> verify(otp));

        assertEquals(OtpFailureReason.EXPIRED, exception.getReason());
        assertFalse(service.isVerified(session, PURPOSE, VerificationChannel.EMAIL, EMAIL));
    }

    @Test
    void captchaRequiredAfterThreeFailedAttempts() {
        service.sendOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, CONTEXT);
        String correctOtp = emailHandler.lastOtp;

        for (int i = 0; i < 2; i++) {
            assertThrows(OtpVerificationException.class, () -> verify("000000"));
        }
        OtpVerificationException thirdFailure = assertThrows(
                OtpVerificationException.class,
                () -> verify("000000"));

        assertTrue(thirdFailure.getResult().captchaRequired());
        assertNotNull(thirdFailure.getResult().captchaId());
        assertNotNull(thirdFailure.getResult().captchaQuestion());

        OtpVerificationException captchaFailure = assertThrows(
                OtpVerificationException.class,
                () -> verify(correctOtp));

        assertEquals(OtpFailureReason.CAPTCHA_REQUIRED, captchaFailure.getReason());
        assertFalse(service.isVerified(session, PURPOSE, VerificationChannel.EMAIL, EMAIL));
    }

    private OtpVerificationResult verify(String otp) {
        return service.verifyOtp(session, PURPOSE, VerificationChannel.EMAIL, EMAIL, otp, null, null, CONTEXT);
    }

    private OtpVerificationResult verifyWrongOtpWithCaptchaWhenRequired() {
        OtpVerificationStateEntity state = currentState();
        if (state == null || state.getCaptchaId() == null) {
            return verify("000000");
        }
        return service.verifyOtp(
                session,
                PURPOSE,
                VerificationChannel.EMAIL,
                EMAIL,
                "000000",
                state.getCaptchaId(),
                solveCaptcha(state.getCaptchaQuestion()),
                CONTEXT);
    }

    private String solveCaptcha(String captchaQuestion) {
        Matcher matcher = Pattern.compile("(\\d+) \\+ (\\d+)").matcher(captchaQuestion);
        if (!matcher.find()) {
            throw new AssertionError("Unexpected CAPTCHA question: " + captchaQuestion);
        }
        return Integer.toString(Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2)));
    }

    private OtpVerificationStateEntity currentState() {
        return stateStore.get(key(session.getId(), PURPOSE, VerificationChannel.EMAIL.name()));
    }

    private static String key(String sessionId, String purpose, String channel) {
        return sessionId + ":" + purpose + ":" + channel;
    }

    private static final class CapturingEmailHandler implements OtpChannelHandler {

        private String lastOtp;
        private String lastOtpReferenceId;

        @Override
        public VerificationChannel getChannel() {
            return VerificationChannel.EMAIL;
        }

        @Override
        public String normalizeReference(String reference) {
            return reference.trim().toLowerCase();
        }

        @Override
        public void dispatchOtp(String purpose, String reference, String otp) {
            lastOtp = otp;
        }

        @Override
        public void dispatchOtp(String purpose, String reference, String otp, String otpReferenceId) {
            lastOtp = otp;
            lastOtpReferenceId = otpReferenceId;
        }
    }

    private static final class CapturingBothHandler implements OtpChannelHandler {

        private String emailOtp;
        private String smsOtp;

        @Override
        public VerificationChannel getChannel() {
            return VerificationChannel.BOTH;
        }

        @Override
        public String normalizeReference(String reference) {
            return OtpDeliveryReferences.both(
                    OtpDeliveryReferences.parseBoth(reference).email(),
                    OtpDeliveryReferences.parseBoth(reference).mobileNumber());
        }

        @Override
        public void dispatchOtp(String purpose, String reference, String otp) {
            dispatchOtp(purpose, reference, otp, null);
        }

        @Override
        public void dispatchOtp(String purpose, String reference, String otp, String otpReferenceId) {
            emailOtp = otp;
            smsOtp = otp;
        }
    }

    private static final class FailingSmsHandler implements OtpChannelHandler {

        @Override
        public VerificationChannel getChannel() {
            return VerificationChannel.SMS;
        }

        @Override
        public String normalizeReference(String reference) {
            return reference.trim();
        }

        @Override
        public void dispatchOtp(String purpose, String reference, String otp) {
            throw new IllegalStateException("SMS failed");
        }
    }
}
