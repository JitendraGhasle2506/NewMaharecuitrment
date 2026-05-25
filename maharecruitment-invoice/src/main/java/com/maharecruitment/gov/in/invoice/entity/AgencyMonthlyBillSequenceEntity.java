package com.maharecruitment.gov.in.invoice.entity;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "agency_monthly_bill_sequence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agency_bill_sequence_period",
                        columnNames = { "agency_id", "bill_year", "bill_month" })
        },
        indexes = {
                @Index(name = "idx_agency_bill_sequence_period",
                        columnList = "agency_id,bill_year,bill_month")
        })
public class AgencyMonthlyBillSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_monthly_bill_sequence_id")
    private Long agencyMonthlyBillSequenceId;

    @Column(name = "agency_id", nullable = false)
    private Long agencyId;

    @Column(name = "bill_year", nullable = false)
    private Integer billYear;

    @Column(name = "bill_month", nullable = false)
    private Integer billMonth;

    @Column(name = "sequence_date")
    private LocalDate sequenceDate;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (lastSequence == null || lastSequence < 0) {
            lastSequence = 0;
        }
    }
}
