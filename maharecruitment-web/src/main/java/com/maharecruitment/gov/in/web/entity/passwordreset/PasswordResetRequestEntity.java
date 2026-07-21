package com.maharecruitment.gov.in.web.entity.passwordreset;

import java.time.Instant;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetStatus;
import com.maharecruitment.gov.in.web.service.passwordreset.ResetPasswordChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "password_reset_request",
        indexes = {
                @Index(name = "idx_password_reset_user", columnList = "user_id"),
                @Index(name = "idx_password_reset_status", columnList = "request_status"),
                @Index(name = "idx_password_reset_otp_expiry", columnList = "otp_expiry_time"),
                @Index(name = "idx_password_reset_token_hash", columnList = "reset_token_hash")
        })
public class PasswordResetRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private ResetPasswordChannel channel;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "otp_expiry_time", nullable = false)
    private Instant otpExpiryTime;

    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified;

    @Column(name = "otp_verified_time")
    private Instant otpVerifiedTime;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expiry_time")
    private Instant resetTokenExpiryTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 30)
    private PasswordResetStatus requestStatus;

    @Column(name = "requested_ip", length = 100)
    private String requestedIp;

    @Column(name = "verified_ip", length = 100)
    private String verifiedIp;

    @Column(name = "reset_ip", length = 100)
    private String resetIp;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    @Column(name = "updated_on")
    private Instant updatedOn;

    @Column(name = "completed_on")
    private Instant completedOn;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdOn == null) {
            createdOn = now;
        }
        updatedOn = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedOn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ResetPasswordChannel getChannel() {
        return channel;
    }

    public void setChannel(ResetPasswordChannel channel) {
        this.channel = channel;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public Instant getOtpExpiryTime() {
        return otpExpiryTime;
    }

    public void setOtpExpiryTime(Instant otpExpiryTime) {
        this.otpExpiryTime = otpExpiryTime;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }

    public Instant getOtpVerifiedTime() {
        return otpVerifiedTime;
    }

    public void setOtpVerifiedTime(Instant otpVerifiedTime) {
        this.otpVerifiedTime = otpVerifiedTime;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public Instant getResetTokenExpiryTime() {
        return resetTokenExpiryTime;
    }

    public void setResetTokenExpiryTime(Instant resetTokenExpiryTime) {
        this.resetTokenExpiryTime = resetTokenExpiryTime;
    }

    public PasswordResetStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(PasswordResetStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getRequestedIp() {
        return requestedIp;
    }

    public void setRequestedIp(String requestedIp) {
        this.requestedIp = requestedIp;
    }

    public String getVerifiedIp() {
        return verifiedIp;
    }

    public void setVerifiedIp(String verifiedIp) {
        this.verifiedIp = verifiedIp;
    }

    public String getResetIp() {
        return resetIp;
    }

    public void setResetIp(String resetIp) {
        this.resetIp = resetIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public Instant getUpdatedOn() {
        return updatedOn;
    }

    public Instant getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Instant completedOn) {
        this.completedOn = completedOn;
    }

    public long getVersion() {
        return version;
    }
}
