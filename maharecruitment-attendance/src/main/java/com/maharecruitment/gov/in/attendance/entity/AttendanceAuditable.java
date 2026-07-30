package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.common.entity.Auditable;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class AttendanceAuditable extends Auditable {

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (getCreatedDate() == null) {
            setCreatedDate(now);
        }
        setUpdatedDate(now);
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedDate(LocalDateTime.now());
    }
}
