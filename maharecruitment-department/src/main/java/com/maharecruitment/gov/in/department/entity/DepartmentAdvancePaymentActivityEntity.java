package com.maharecruitment.gov.in.department.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "department_advance_payment_activity",
        indexes = {
                @Index(name = "idx_dep_adv_pay_activity_pay_id", columnList = "department_advance_payment_id"),
                @Index(name = "idx_dep_adv_pay_activity_time", columnList = "action_timestamp")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAdvancePaymentActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_advance_payment_id", nullable = false)
    private DepartmentAdvancePaymentEntity payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private DepartmentApplicationActivityType activityType;

    @Convert(converter = DepartmentApplicationStatusConverter.class)
    @Column(name = "previous_status", length = 30)
    private DepartmentApplicationStatus previousStatus;

    @Convert(converter = DepartmentApplicationStatusConverter.class)
    @Column(name = "new_status", length = 30)
    private DepartmentApplicationStatus newStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 120)
    private String actorEmail;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "activity_remarks", columnDefinition = "TEXT")
    private String activityRemarks;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp;

    @PrePersist
    void onCreate() {
        if (actionTimestamp == null) {
            actionTimestamp = LocalDateTime.now();
        }

        if (actorEmail != null) {
            actorEmail = actorEmail.trim().toLowerCase();
        }
        if (actorName != null) {
            actorName = actorName.trim();
        }
        if (activityRemarks != null) {
            activityRemarks = activityRemarks.trim();
        }
    }
}
