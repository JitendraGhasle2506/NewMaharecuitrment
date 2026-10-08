package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_recruitment_type_rule")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRecruitmentTypeRuleEntity {

    @Id
    @Column(name = "recruitment_type_code", length = 30)
    private String recruitmentTypeCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void normalizeCode() {
        if (recruitmentTypeCode != null) {
            recruitmentTypeCode = recruitmentTypeCode.trim().toUpperCase(Locale.ROOT);
        }
    }
}
