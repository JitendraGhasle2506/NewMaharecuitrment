package com.maharecruitment.gov.in.invoice.entity;

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
        name = "department_tax_invoice_sequence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dep_tax_invoice_sequence_fy", columnNames = "financial_year_code")
        },
        indexes = {
                @Index(name = "idx_dep_tax_invoice_sequence_fy", columnList = "financial_year_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTaxInvoiceSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_tax_invoice_sequence_id")
    private Long departmentTaxInvoiceSequenceId;

    @Column(name = "financial_year_code", nullable = false, length = 9)
    private String financialYearCode;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (financialYearCode != null) {
            financialYearCode = financialYearCode.trim();
        }
        if (lastSequence == null || lastSequence < 0) {
            lastSequence = 0;
        }
    }
}
