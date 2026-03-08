package com.maharecruitment.gov.in.web.service.verification.impl;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;

import jakarta.servlet.http.HttpSession;

@Service
public class OtpVerificationServiceImpl implements OtpVerificationService {

    private static final String SESSION_KEY = "otp.verification.state";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<VerificationChannel, OtpChannelHandler> handlers;
    private final OtpVerificationProperties properties;

    public OtpVerificationServiceImpl(
            List<OtpChannelHandler> handlers,
            OtpVerificationProperties properties) {
        this.handlers = new EnumMap<>(VerificationChannel.class);
        handlers.forEach(handler -> this.handlers.put(handler.getChannel(), handler));
        this.properties = properties;
    }

    @Override
    public void sendOtp(HttpSession session, String purpose, VerificationChannel channel, String reference) {
        OtpChannelHandler handler = getHandler(channel);
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedReference = handler.normalizeReference(reference);
        String otp = generateOtp();
        VerificationState pendingState = VerificationState.pending(normalizedReference, hash(otp), expiry());

        try {
            handler.dispatchOtp(normalizedReference, otp);
            getSessionStore(session).put(toStateKey(normalizedPurpose, channel), pendingState);
        } catch (RuntimeException ex) {
            getSessionStore(session).remove(toStateKey(normalizedPurpose, channel));
            throw ex;
        }
    }

    @Override
    public boolean verifyOtp(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            String reference,
            String otp) {
        OtpChannelHandler handler = getHandler(channel);
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedReference = handler.normalizeReference(reference);
        VerificationState state = getSessionStore(session).get(toStateKey(normalizedPurpose, channel));

        validateState(session, normalizedPurpose, channel, state, normalizedReference, otp);
        getSessionStore(session).put(toStateKey(normalizedPurpose, channel), state.markVerified());
        return true;
    }

    @Override
    public boolean isVerified(HttpSession session, String purpose, VerificationChannel channel, String reference) {
        if (session == null || channel == null || !StringUtils.hasText(purpose) || !StringUtils.hasText(reference)) {
            return false;
        }

        try {
            OtpChannelHandler handler = getHandler(channel);
            String normalizedReference = handler.normalizeReference(reference);
            VerificationState state = getSessionStore(session).get(toStateKey(normalizePurpose(purpose), channel));
            return state != null
                    && !state.isExpired()
                    && state.verified()
                    && state.reference().equals(normalizedReference);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public void clear(HttpSession session, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        Map<String, VerificationState> store = getSessionStore(session);
        for (VerificationChannel channel : VerificationChannel.values()) {
            store.remove(toStateKey(normalizedPurpose, channel));
        }
    }

    @Override
    public void clear(HttpSession session, String purpose, VerificationChannel channel) {
        getSessionStore(session).remove(toStateKey(normalizePurpose(purpose), channel));
    }

    private void validateState(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            VerificationState state,
            String expectedReference,
            String otp) {
        if (state == null) {
            throw new IllegalArgumentException("OTP has not been requested for the current value.");
        }
        if (!StringUtils.hasText(otp)) {
            throw new IllegalArgumentException("OTP is required.");
        }
        if (state.isExpired()) {
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }
        if (!state.reference().equals(expectedReference)) {
            throw new IllegalArgumentException("OTP does not match the current value.");
        }
        if (state.attempts() >= properties.getMaxAttempts()) {
            throw new IllegalArgumentException("Maximum OTP verification attempts exceeded.");
        }
        if (!state.matches(hash(otp.trim()))) {
            getSessionStore(session).put(
                    toStateKey(purpose, channel),
                    state.incrementFailedAttempt());
            throw new IllegalArgumentException("Invalid OTP.");
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, VerificationState> getSessionStore(HttpSession session) {
        Object attribute = session.getAttribute(SESSION_KEY);
        if (attribute instanceof Map<?, ?> existingStore) {
            return (Map<String, VerificationState>) existingStore;
        }

        Map<String, VerificationState> newStore = new HashMap<>();
        session.setAttribute(SESSION_KEY, newStore);
        return newStore;
    }

    private Instant expiry() {
        return Instant.now().plusSeconds(properties.getExpirySeconds());
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

    private String toStateKey(String purpose, VerificationChannel channel) {
        return purpose + ":" + channel.name();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash OTP.", ex);
        }
    }

    private record VerificationState(
            String reference,
            String otpHash,
            Instant expiresAt,
            boolean verified,
            int attempts) implements Serializable {

        private static VerificationState pending(String reference, String otpHash, Instant expiresAt) {
            return new VerificationState(reference, otpHash, expiresAt, false, 0);
        }

        private VerificationState markVerified() {
            return new VerificationState(reference, otpHash, expiresAt, true, attempts + 1);
        }

        private VerificationState incrementFailedAttempt() {
            return new VerificationState(reference, otpHash, expiresAt, false, attempts + 1);
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        private boolean matches(String submittedHash) {
            return otpHash.equals(submittedHash);
        }
    }
}
