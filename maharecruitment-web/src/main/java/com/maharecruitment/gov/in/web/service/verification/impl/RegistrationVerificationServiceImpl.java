package com.maharecruitment.gov.in.web.service.verification.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.service.verification.RegistrationOtpNotificationService;
import com.maharecruitment.gov.in.web.service.verification.RegistrationVerificationService;

import jakarta.servlet.http.HttpSession;

@Service
public class RegistrationVerificationServiceImpl implements RegistrationVerificationService {

    private static final String MOBILE_SESSION_KEY = "registration.mobile.verification";
    private static final String EMAIL_SESSION_KEY = "registration.email.verification";
    private static final int OTP_EXPIRY_SECONDS = 10 * 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationOtpNotificationService notificationService;

    public RegistrationVerificationServiceImpl(RegistrationOtpNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void sendMobileOtp(HttpSession session, String mobileNo) {
        String normalizedMobile = normalizeMobile(mobileNo);
        String otp = generateOtp();
        session.setAttribute(MOBILE_SESSION_KEY, VerificationState.pending(normalizedMobile, hash(otp), expiry()));
        notificationService.sendMobileOtp(normalizedMobile, otp);
    }

    @Override
    public boolean verifyMobileOtp(HttpSession session, String mobileNo, String otp) {
        VerificationState state = getState(session, MOBILE_SESSION_KEY);
        validateState(session, MOBILE_SESSION_KEY, state, normalizeMobile(mobileNo), otp);
        session.setAttribute(MOBILE_SESSION_KEY, state.markVerified());
        return true;
    }

    @Override
    public boolean isMobileVerified(HttpSession session, String mobileNo) {
        return isVerified(getState(session, MOBILE_SESSION_KEY), normalizeMobile(mobileNo));
    }

    @Override
    public void sendEmailOtp(HttpSession session, String email) {
        String normalizedEmail = normalizeEmail(email);
        String otp = generateOtp();
        session.setAttribute(EMAIL_SESSION_KEY, VerificationState.pending(normalizedEmail, hash(otp), expiry()));
        notificationService.sendEmailOtp(normalizedEmail, otp);
    }

    @Override
    public boolean verifyEmailOtp(HttpSession session, String email, String otp) {
        VerificationState state = getState(session, EMAIL_SESSION_KEY);
        validateState(session, EMAIL_SESSION_KEY, state, normalizeEmail(email), otp);
        session.setAttribute(EMAIL_SESSION_KEY, state.markVerified());
        return true;
    }

    @Override
    public boolean isEmailVerified(HttpSession session, String email) {
        return isVerified(getState(session, EMAIL_SESSION_KEY), normalizeEmail(email));
    }

    @Override
    public void clear(HttpSession session) {
        session.removeAttribute(MOBILE_SESSION_KEY);
        session.removeAttribute(EMAIL_SESSION_KEY);
    }

    private void validateState(
            HttpSession session,
            String sessionKey,
            VerificationState state,
            String expectedReference,
            String otp) {
        if (state == null || !StringUtils.hasText(expectedReference)) {
            throw new IllegalArgumentException("OTP has not been requested for the current value.");
        }
        if (state.isExpired()) {
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }
        if (!state.reference().equals(expectedReference)) {
            throw new IllegalArgumentException("OTP does not match the current value.");
        }
        if (state.attempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Maximum OTP verification attempts exceeded.");
        }
        if (!state.matches(hash(otp))) {
            session.setAttribute(sessionKey, state.incrementFailedAttempt());
            throw new IllegalArgumentException("Invalid OTP.");
        }
    }

    private boolean isVerified(VerificationState state, String reference) {
        return state != null
                && !state.isExpired()
                && state.verified()
                && state.reference().equals(reference);
    }

    private VerificationState getState(HttpSession session, String key) {
        Object attribute = session.getAttribute(key);
        return attribute instanceof VerificationState state ? state : null;
    }

    private Instant expiry() {
        return Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);
    }

    private String generateOtp() {
        int value = RANDOM.nextInt(900000) + 100000;
        return Integer.toString(value);
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

    private String normalizeMobile(String mobileNo) {
        if (!StringUtils.hasText(mobileNo)) {
            return "";
        }
        String normalized = mobileNo.trim();
        if (!normalized.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record VerificationState(
            String reference,
            String otpHash,
            Instant expiresAt,
            boolean verified,
            int attempts) implements java.io.Serializable {

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
