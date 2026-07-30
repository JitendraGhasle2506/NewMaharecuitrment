package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores Firebase Cloud Messaging tokens registered by employee mobile devices.
 */
@Entity
@Table(
        name = "employee_fcm_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_fcm_tokens_employee_device",
                        columnNames = { "employee_id", "device_id" })
        },
        indexes = {
                @Index(name = "idx_employee_fcm_tokens_employee_id", columnList = "employee_id"),
                @Index(name = "idx_employee_fcm_tokens_device_id", columnList = "device_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "fcm_token", nullable = false, columnDefinition = "text")
    private String fcmToken;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
