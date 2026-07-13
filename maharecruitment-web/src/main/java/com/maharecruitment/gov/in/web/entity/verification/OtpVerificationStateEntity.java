package com.maharecruitment.gov.in.web.entity.verification;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "otp_verification_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_otp_state_session_purpose_channel",
                        columnNames = {"session_id", "purpose", "channel"})
        },
        indexes = {
                @Index(name = "idx_otp_state_reference", columnList = "purpose, channel, reference_hash"),
                @Index(name = "idx_otp_state_locked_until", columnList = "otp_locked_until"),
                @Index(name = "idx_otp_state_expiry", columnList = "otp_expiry_time")
        })
public class OtpVerificationStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_state_id")
    private Long otpStateId;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "purpose", nullable = false, length = 120)
    private String purpose;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "reference_hash", nullable = false, length = 64)
    private String referenceHash;

    @Column(name = "reference_masked", length = 120)
    private String referenceMasked;

    @Column(name = "otp_hash", length = 64)
    private String otpHash;

    @Column(name = "otp_attempt_count", nullable = false)
    private int otpAttemptCount;

    @Column(name = "otp_locked_until")
    private Instant otpLockedUntil;

    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified;

    @Column(name = "otp_expiry_time")
    private Instant otpExpiryTime;

    @Column(name = "otp_resend_count", nullable = false)
    private int otpResendCount;

    @Column(name = "otp_resend_window_start")
    private Instant otpResendWindowStart;

    @Column(name = "otp_last_sent_at")
    private Instant otpLastSentAt;

    @Column(name = "captcha_id", length = 64)
    private String captchaId;

    @Column(name = "captcha_answer_hash", length = 64)
    private String captchaAnswerHash;

    @Column(name = "captcha_question", length = 120)
    private String captchaQuestion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getOtpStateId() {
        return otpStateId;
    }

    public void setOtpStateId(Long otpStateId) {
        this.otpStateId = otpStateId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReferenceHash() {
        return referenceHash;
    }

    public void setReferenceHash(String referenceHash) {
        this.referenceHash = referenceHash;
    }

    public String getReferenceMasked() {
        return referenceMasked;
    }

    public void setReferenceMasked(String referenceMasked) {
        this.referenceMasked = referenceMasked;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public int getOtpAttemptCount() {
        return otpAttemptCount;
    }

    public void setOtpAttemptCount(int otpAttemptCount) {
        this.otpAttemptCount = otpAttemptCount;
    }

    public Instant getOtpLockedUntil() {
        return otpLockedUntil;
    }

    public void setOtpLockedUntil(Instant otpLockedUntil) {
        this.otpLockedUntil = otpLockedUntil;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }

    public Instant getOtpExpiryTime() {
        return otpExpiryTime;
    }

    public void setOtpExpiryTime(Instant otpExpiryTime) {
        this.otpExpiryTime = otpExpiryTime;
    }

    public int getOtpResendCount() {
        return otpResendCount;
    }

    public void setOtpResendCount(int otpResendCount) {
        this.otpResendCount = otpResendCount;
    }

    public Instant getOtpResendWindowStart() {
        return otpResendWindowStart;
    }

    public void setOtpResendWindowStart(Instant otpResendWindowStart) {
        this.otpResendWindowStart = otpResendWindowStart;
    }

    public Instant getOtpLastSentAt() {
        return otpLastSentAt;
    }

    public void setOtpLastSentAt(Instant otpLastSentAt) {
        this.otpLastSentAt = otpLastSentAt;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaAnswerHash() {
        return captchaAnswerHash;
    }

    public void setCaptchaAnswerHash(String captchaAnswerHash) {
        this.captchaAnswerHash = captchaAnswerHash;
    }

    public String getCaptchaQuestion() {
        return captchaQuestion;
    }

    public void setCaptchaQuestion(String captchaQuestion) {
        this.captchaQuestion = captchaQuestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
