package com.maharecruitment.gov.in.attendance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "manual_attendance_requests")
public class ManualAttendanceRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "in_time")
    private String inTime;

    @Column(name = "out_time")
    private String outTime;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "hod_id")
    private Long hodId;

    // e.g., PENDING, APPROVED, REJECTED
    @Column(name = "manager_status", length = 20)
    private String managerStatus = "PENDING";

    // e.g., PENDING, APPROVED, REJECTED
    @Column(name = "hod_status", length = 20)
    private String hodStatus = "PENDING";

    @Column(name = "manager_comments", length = 500)
    private String managerComments;

    @Column(name = "hod_comments", length = 500)
    private String hodComments;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
