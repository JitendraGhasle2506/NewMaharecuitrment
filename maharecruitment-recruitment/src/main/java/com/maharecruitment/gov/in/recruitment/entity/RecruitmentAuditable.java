package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class RecruitmentAuditable {

    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "updated_date_time", nullable = false)
    private LocalDateTime updatedDateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDateTime == null) {
            createdDateTime = now;
        }
        updatedDateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDateTime = LocalDateTime.now();
    }
}
