package com.maharecruitment.gov.in.web.service.verification.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.entity.verification.OtpVerificationStateEntity;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.repository.verification.OtpVerificationStateRepository;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpFailureReason;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimitException;
import com.maharecruitment.gov.in.web.service.verification.OtpRateLimiter;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpSecurityAuditService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationException;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;

import jakarta.servlet.http.HttpSession;

@Service
@Transactional
public class DatabaseOtpVerificationService implements OtpVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<VerificationChannel, OtpChannelHandler> handlers;
    private final OtpVerificationProperties properties;
    private final OtpVerificationStateRepository stateRepository;
    private final OtpRateLimiter rateLimiter;
    private final OtpSecurityAuditService auditService;

    public DatabaseOtpVerificationService(
            List<OtpChannelHandler> handlers,
            OtpVerificationProperties properties,
            OtpVerificationStateRepository stateRepository,
            OtpRateLimiter rateLimiter,
            OtpSecurityAuditService auditService) {
        this.handlers = new java.util.EnumMap<>(VerificationChannel.class);
        handlers.forEach(handler -> this.handlers.put(handler.getChannel(), handler));
        this.properties = properties;
        this.stateRepository = stateRepository;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    @Override
    public OtpVerificationResult sendOtp(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        OtpChannelHandler handler = getHandler(channel);
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedReference = handler.normalizeReference(reference);
        OtpRequestContext requestContext = normalizeContext(context);

        rateLimiter.checkSendAllowed(normalizedPurpose, channel, normalizedReference, requestContext);

        Instant now = Instant.now();
        OtpVerificationStateEntity state = findState(session, normalizedPurpose, channel)
                .orElseGet(() -> newState(session, normalizedPurpose, channel));
        boolean resend = state.getOtpStateId() != null && state.getOtpExpiryTime() != null;
        int resendCount = nextResendCount(state, now);

        String otp = generateOtp();
        state.setReferenceHash(hash(normalizedReference));
        state.setReferenceMasked(maskReference(channel, normalizedReference));
        state.setOtpHash(hash(otp));
        state.setOtpAttemptCount(0);
        state.setOtpLockedUntil(null);
        state.setOtpVerified(false);
        state.setOtpExpiryTime(now.plusSeconds(properties.getExpirySeconds()));
        state.setOtpResendCount(resendCount);
        clearCaptcha(state);

        handler.dispatchOtp(normalizedPurpose, normalizedReference, otp);
        stateRepository.save(state);

        auditService.record(
                resend ? "OTP_RESEND" : "OTP_SENT",
                normalizedPurpose,
                channel,
                state.getReferenceMasked(),
                requestContext,
                null,
                Map.of("remainingResends", Math.max(0, properties.getResendLimit() - resendCount)));

        return OtpVerificationResult.sent(
                properties.getResendLimit() - resendCount,
                properties.getExpirySeconds());
    }

    @Override
    public OtpVerificationResult verifyOtp(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            String reference,
            String otp,
            String captchaId,
            String captchaAnswer,
            OtpRequestContext context) {
        OtpChannelHandler handler = getHandler(channel);
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedReference = handler.normalizeReference(reference);
        OtpRequestContext requestContext = normalizeContext(context);

        rateLimiter.checkVerifyAllowed(normalizedPurpose, channel, normalizedReference, requestContext);

        OtpVerificationStateEntity state = findState(session, normalizedPurpose, channel).orElse(null);
        if (state == null) {
            throw failure(
                    OtpFailureReason.NOT_REQUESTED,
                    "OTP not requested.",
                    normalizedPurpose,
                    channel,
                    maskReference(channel, normalizedReference),
                    requestContext,
                    OtpVerificationResult.failed(properties.getMaxAttempts(), false, null, null));
        }

        Instant now = Instant.now();
        String maskedReference = state.getReferenceMasked();

        if (isLocked(state, now)) {
            throw failure(
                    OtpFailureReason.LOCKED,
                    "OTP verification locked until " + state.getOtpLockedUntil() + ".",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    lockedResult(state, now));
        }

        if (lockExpired(state, now)) {
            state.setOtpLockedUntil(null);
            stateRepository.save(state);
        }

        if (isExpired(state, now)) {
            invalidateOtp(state);
            stateRepository.save(state);
            throw failure(
                    OtpFailureReason.EXPIRED,
                    "OTP expired at " + state.getOtpExpiryTime() + ".",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    OtpVerificationResult.failed(0, false, null, null),
                    "OTP_EXPIRED");
        }

        if (!hash(normalizedReference).equals(state.getReferenceHash())) {
            throw failure(
                    OtpFailureReason.REFERENCE_MISMATCH,
                    "OTP reference mismatch.",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    failedResult(state));
        }

        if (!StringUtils.hasText(otp)) {
            throw failure(
                    OtpFailureReason.INVALID_OTP,
                    "OTP value missing.",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    failedResult(state));
        }

        if (isCaptchaRequired(state) && !isCaptchaValid(state, captchaId, captchaAnswer)) {
            ensureCaptchaChallenge(state);
            stateRepository.save(state);
            OtpFailureReason reason = StringUtils.hasText(captchaAnswer)
                    ? OtpFailureReason.CAPTCHA_INVALID
                    : OtpFailureReason.CAPTCHA_REQUIRED;
            throw failure(
                    reason,
                    "CAPTCHA validation failed before OTP verification.",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    failedResult(state));
        }

        if (!StringUtils.hasText(state.getOtpHash())) {
            throw failure(
                    OtpFailureReason.INVALIDATED,
                    "OTP has already been invalidated.",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    failedResult(state));
        }

        if (constantTimeEquals(state.getOtpHash(), hash(otp.trim()))) {
            state.setOtpVerified(true);
            state.setOtpAttemptCount(0);
            state.setOtpLockedUntil(null);
            state.setOtpHash(null);
            clearCaptcha(state);
            stateRepository.save(state);

            auditService.record(
                    "OTP_VERIFICATION_SUCCESS",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    null,
                    Map.of("remainingAttempts", properties.getMaxAttempts()));
            return OtpVerificationResult.verified(properties.getMaxAttempts());
        }

        int attempts = Math.min(properties.getMaxAttempts(), state.getOtpAttemptCount() + 1);
        state.setOtpAttemptCount(attempts);
        state.setOtpVerified(false);

        if (attempts >= properties.getMaxAttempts()) {
            state.setOtpLockedUntil(now.plusSeconds(properties.getLockDurationSeconds()));
            invalidateOtp(state);
            stateRepository.save(state);

            auditService.record(
                    "OTP_VERIFICATION_FAILED",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    "Invalid OTP. Maximum attempts reached.",
                    Map.of("attemptCount", attempts));
            auditService.record(
                    "OTP_LOCKED",
                    normalizedPurpose,
                    channel,
                    maskedReference,
                    requestContext,
                    "OTP locked after maximum failed attempts.",
                    Map.of("lockedUntil", state.getOtpLockedUntil().toString()));
            throw new OtpVerificationException(
                    OtpFailureReason.LOCKED,
                    "OTP locked after maximum failed attempts.",
                    lockedResult(state, now));
        }

        if (isCaptchaRequired(state)) {
            ensureCaptchaChallenge(state);
        }
        stateRepository.save(state);

        throw failure(
                OtpFailureReason.INVALID_OTP,
                "Invalid OTP.",
                normalizedPurpose,
                channel,
                maskedReference,
                requestContext,
                failedResult(state));
    }

    @Override
    public void recordUnknownSendAttempt(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        String normalizedPurpose = normalizePurpose(purpose);
        OtpRequestContext requestContext = normalizeContext(context);
        String normalizedReference = normalizeGenericReference(reference);
        rateLimiter.checkSendAllowed(normalizedPurpose, channel, normalizedReference, requestContext);
        auditService.record(
                "OTP_SEND_SUPPRESSED",
                normalizedPurpose,
                channel,
                maskGenericReference(normalizedReference),
                requestContext,
                "No active account found for OTP send request.",
                null);
    }

    @Override
    public void recordUnknownVerifyAttempt(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context) {
        String normalizedPurpose = normalizePurpose(purpose);
        OtpRequestContext requestContext = normalizeContext(context);
        String normalizedReference = normalizeGenericReference(reference);
        rateLimiter.checkVerifyAllowed(normalizedPurpose, channel, normalizedReference, requestContext);
        throw failure(
                OtpFailureReason.NOT_REQUESTED,
                "No active account found for OTP verification request.",
                normalizedPurpose,
                channel,
                maskGenericReference(normalizedReference),
                requestContext,
                OtpVerificationResult.failed(properties.getMaxAttempts(), false, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(HttpSession session, String purpose, VerificationChannel channel, String reference) {
        if (session == null || channel == null || !StringUtils.hasText(purpose) || !StringUtils.hasText(reference)) {
            return false;
        }

        try {
            OtpChannelHandler handler = getHandler(channel);
            String normalizedReference = handler.normalizeReference(reference);
            Instant now = Instant.now();
            return findState(session, normalizePurpose(purpose), channel)
                    .filter(state -> state.isOtpVerified())
                    .filter(state -> !isExpired(state, now))
                    .filter(state -> hash(normalizedReference).equals(state.getReferenceHash()))
                    .isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public void clear(HttpSession session, String purpose) {
        if (session == null || !StringUtils.hasText(purpose)) {
            return;
        }
        stateRepository.deleteBySessionIdAndPurpose(session.getId(), normalizePurpose(purpose));
    }

    @Override
    public void clear(HttpSession session, String purpose, VerificationChannel channel) {
        if (session == null || channel == null || !StringUtils.hasText(purpose)) {
            return;
        }
        stateRepository.deleteBySessionIdAndPurposeAndChannel(
                session.getId(),
                normalizePurpose(purpose),
                channel.name());
    }

    private OtpChannelHandler getHandler(VerificationChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Verification channel is required.");
        }

        OtpChannelHandler handler = handlers.get(channel);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported verification channel.");
        }
        return handler;
    }

    private java.util.Optional<OtpVerificationStateEntity> findState(
            HttpSession session,
            String purpose,
            VerificationChannel channel) {
        if (session == null) {
            return java.util.Optional.empty();
        }
        return stateRepository.findBySessionIdAndPurposeAndChannel(session.getId(), purpose, channel.name());
    }

    private OtpVerificationStateEntity newState(HttpSession session, String purpose, VerificationChannel channel) {
        OtpVerificationStateEntity state = new OtpVerificationStateEntity();
        state.setSessionId(session.getId());
        state.setPurpose(purpose);
        state.setChannel(channel.name());
        return state;
    }

    private int nextResendCount(OtpVerificationStateEntity state, Instant now) {
        Instant windowStart = state.getOtpResendWindowStart();
        Duration resendWindow = Duration.ofMinutes(Math.max(1, properties.getResendWindowMinutes()));
        if (windowStart == null || !now.isBefore(windowStart.plus(resendWindow))) {
            state.setOtpResendWindowStart(now);
            return 1;
        }

        if (state.getOtpResendCount() >= properties.getResendLimit()) {
            long retryAfterSeconds = secondsUntil(now, windowStart.plus(resendWindow));
            throw new OtpRateLimitException("OTP resend limit exceeded.", retryAfterSeconds);
        }

        return state.getOtpResendCount() + 1;
    }

    private void invalidateOtp(OtpVerificationStateEntity state) {
        state.setOtpHash(null);
        state.setOtpVerified(false);
    }

    private boolean isExpired(OtpVerificationStateEntity state, Instant now) {
        return state.getOtpExpiryTime() == null || !now.isBefore(state.getOtpExpiryTime());
    }

    private boolean isLocked(OtpVerificationStateEntity state, Instant now) {
        return state.getOtpLockedUntil() != null && now.isBefore(state.getOtpLockedUntil());
    }

    private boolean lockExpired(OtpVerificationStateEntity state, Instant now) {
        return state.getOtpLockedUntil() != null && !now.isBefore(state.getOtpLockedUntil());
    }

    private boolean isCaptchaRequired(OtpVerificationStateEntity state) {
        return !state.isOtpVerified()
                && state.getOtpAttemptCount() >= Math.max(1, properties.getCaptchaThreshold());
    }

    private void ensureCaptchaChallenge(OtpVerificationStateEntity state) {
        if (StringUtils.hasText(state.getCaptchaId())
                && StringUtils.hasText(state.getCaptchaAnswerHash())
                && StringUtils.hasText(state.getCaptchaQuestion())) {
            return;
        }

        int first = RANDOM.nextInt(8) + 2;
        int second = RANDOM.nextInt(8) + 2;
        String answer = Integer.toString(first + second);
        state.setCaptchaId(UUID.randomUUID().toString());
        state.setCaptchaQuestion("What is " + first + " + " + second + "?");
        state.setCaptchaAnswerHash(hash(answer));
    }

    private boolean isCaptchaValid(OtpVerificationStateEntity state, String captchaId, String captchaAnswer) {
        if (!StringUtils.hasText(state.getCaptchaId()) || !StringUtils.hasText(state.getCaptchaAnswerHash())) {
            ensureCaptchaChallenge(state);
            return false;
        }
        return state.getCaptchaId().equals(captchaId)
                && StringUtils.hasText(captchaAnswer)
                && constantTimeEquals(state.getCaptchaAnswerHash(), hash(captchaAnswer.trim()));
    }

    private void clearCaptcha(OtpVerificationStateEntity state) {
        state.setCaptchaId(null);
        state.setCaptchaQuestion(null);
        state.setCaptchaAnswerHash(null);
    }

    private OtpVerificationResult failedResult(OtpVerificationStateEntity state) {
        if (isLocked(state, Instant.now())) {
            return lockedResult(state, Instant.now());
        }
        if (isCaptchaRequired(state)) {
            ensureCaptchaChallenge(state);
        }
        return OtpVerificationResult.failed(
                properties.getMaxAttempts() - state.getOtpAttemptCount(),
                isCaptchaRequired(state),
                state.getCaptchaId(),
                state.getCaptchaQuestion());
    }

    private OtpVerificationResult lockedResult(OtpVerificationStateEntity state, Instant now) {
        return OtpVerificationResult.locked(
                state.getOtpLockedUntil(),
                secondsUntil(now, state.getOtpLockedUntil()),
                false,
                null,
                null);
    }

    private OtpVerificationException failure(
            OtpFailureReason reason,
            String detailMessage,
            String purpose,
            VerificationChannel channel,
            String maskedReference,
            OtpRequestContext context,
            OtpVerificationResult result) {
        return failure(
                reason,
                detailMessage,
                purpose,
                channel,
                maskedReference,
                context,
                result,
                "OTP_VERIFICATION_FAILED");
    }

    private OtpVerificationException failure(
            OtpFailureReason reason,
            String detailMessage,
            String purpose,
            VerificationChannel channel,
            String maskedReference,
            OtpRequestContext context,
            OtpVerificationResult result,
            String auditAction) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("remainingAttempts", result.remainingAttempts());
        metadata.put("captchaRequired", result.captchaRequired());
        if (result.lockedUntil() != null) {
            metadata.put("lockedUntil", result.lockedUntil().toString());
        }
        auditService.record(auditAction, purpose, channel, maskedReference, context, detailMessage, metadata);
        return new OtpVerificationException(reason, detailMessage, result);
    }

    private String generateOtp() {
        int otpLength = properties.getOtpLength();
        if (otpLength < 4) {
            throw new IllegalStateException("OTP length must be at least 4 digits.");
        }
        StringBuilder builder = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            int nextDigit = RANDOM.nextInt(10);
            if (i == 0 && nextDigit == 0) {
                nextDigit = RANDOM.nextInt(9) + 1;
            }
            builder.append(nextDigit);
        }
        return builder.toString();
    }

    private String normalizePurpose(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            throw new IllegalArgumentException("Verification purpose is required.");
        }
        return purpose.trim().toLowerCase(Locale.ROOT);
    }

    private OtpRequestContext normalizeContext(OtpRequestContext context) {
        return context == null ? new OtpRequestContext("unknown") : context;
    }

    private String normalizeGenericReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return "unknown";
        }
        return reference.trim().toLowerCase(Locale.ROOT);
    }

    private String maskReference(VerificationChannel channel, String reference) {
        if (!StringUtils.hasText(reference)) {
            return "unknown";
        }
        if (channel == VerificationChannel.MOBILE) {
            String trimmed = reference.trim();
            return trimmed.length() <= 4
                    ? "****"
                    : "******" + trimmed.substring(trimmed.length() - 4);
        }
        return maskGenericReference(reference);
    }

    private String maskGenericReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return "unknown";
        }
        String normalized = reference.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex > 1) {
            return normalized.charAt(0) + "***" + normalized.substring(atIndex);
        }
        return normalized.length() <= 4
                ? "****"
                : normalized.charAt(0) + "***" + normalized.substring(normalized.length() - 2);
    }

    private long secondsUntil(Instant now, Instant target) {
        if (target == null) {
            return 0;
        }
        return Math.max(1, Duration.between(now, target).getSeconds());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedValue = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedValue);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash OTP value.", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String submitted) {
        if (expected == null || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
