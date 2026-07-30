package com.maharecruitment.gov.in.workorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "work_order_sequence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_order_sequence_key", columnNames = "sequence_key")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_sequence_id")
    private Long workOrderSequenceId;

    @Column(name = "sequence_key", nullable = false, length = 50)
    private String sequenceKey;

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (sequenceKey != null) {
            sequenceKey = sequenceKey.trim().toUpperCase();
        }
        if (lastSequence == null || lastSequence < 0) {
            lastSequence = 0L;
        }
    }
}
