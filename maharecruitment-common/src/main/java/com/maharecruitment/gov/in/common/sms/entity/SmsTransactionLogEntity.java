package com.maharecruitment.gov.in.common.sms.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "sms_transaction_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sms_transaction_correlation",
                        columnNames = "correlation_id")
        },
        indexes = {
                @Index(name = "idx_sms_transaction_user", columnList = "user_id"),
                @Index(name = "idx_sms_transaction_status", columnList = "status"),
                @Index(name = "idx_sms_transaction_requested", columnList = "requested_on")
        })
public class SmsTransactionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sms_transaction_id")
    private Long smsTransactionId;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "mobile_number_masked", nullable = false, length = 20)
    private String mobileNumberMasked;

    @Column(name = "sms_type", nullable = false, length = 50)
    private String smsType;

    @Column(name = "template_id", nullable = false, length = 50)
    private String templateId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "provider_response", length = 1000)
    private String providerResponse;

    @Column(name = "requested_on", nullable = false)
    private Instant requestedOn;

    @Column(name = "sent_on")
    private Instant sentOn;

    @Column(name = "failed_on")
    private Instant failedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (requestedOn == null) {
            requestedOn = now;
        }
        if (createdOn == null) {
            createdOn = now;
        }
    }

    public Long getSmsTransactionId() {
        return smsTransactionId;
    }

    public void setSmsTransactionId(Long smsTransactionId) {
        this.smsTransactionId = smsTransactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMobileNumberMasked() {
        return mobileNumberMasked;
    }

    public void setMobileNumberMasked(String mobileNumberMasked) {
        this.mobileNumberMasked = mobileNumberMasked;
    }

    public String getSmsType() {
        return smsType;
    }

    public void setSmsType(String smsType) {
        this.smsType = smsType;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderResponse() {
        return providerResponse;
    }

    public void setProviderResponse(String providerResponse) {
        this.providerResponse = providerResponse;
    }

    public Instant getRequestedOn() {
        return requestedOn;
    }

    public void setRequestedOn(Instant requestedOn) {
        this.requestedOn = requestedOn;
    }

    public Instant getSentOn() {
        return sentOn;
    }

    public void setSentOn(Instant sentOn) {
        this.sentOn = sentOn;
    }

    public Instant getFailedOn() {
        return failedOn;
    }

    public void setFailedOn(Instant failedOn) {
        this.failedOn = failedOn;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }
}
