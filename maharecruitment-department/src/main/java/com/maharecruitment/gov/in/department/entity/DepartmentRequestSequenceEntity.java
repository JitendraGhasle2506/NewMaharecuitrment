package com.maharecruitment.gov.in.department.entity;

import java.time.LocalDate;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "department_request_sequence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dep_request_sequence_date_type",
                        columnNames = { "sequence_date", "request_type_code" })
        },
        indexes = {
                @Index(name = "idx_dep_request_sequence_date_type", columnList = "sequence_date,request_type_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequestSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_request_sequence_id")
    private Long departmentRequestSequenceId;

    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    @Column(name = "request_type_code", nullable = false, length = 1)
    private String requestTypeCode;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (requestTypeCode != null) {
            requestTypeCode = requestTypeCode.trim().toUpperCase();
        }
        if (lastSequence == null || lastSequence < 0) {
            lastSequence = 0;
        }
    }
}
