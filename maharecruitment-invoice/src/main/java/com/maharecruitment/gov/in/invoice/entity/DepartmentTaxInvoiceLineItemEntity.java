package com.maharecruitment.gov.in.invoice.entity;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "department_tax_invoice_line_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dep_tax_invoice_line_item_row",
                        columnNames = { "department_tax_invoice_id", "line_no" })
        },
        indexes = {
                @Index(name = "idx_dep_tax_invoice_line_item_invoice_id", columnList = "department_tax_invoice_id"),
                @Index(name = "idx_dep_tax_invoice_line_item_requirement_id",
                        columnList = "department_project_resource_requirement_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentTaxInvoiceLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_tax_invoice_line_item_id")
    private Long departmentTaxInvoiceLineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_tax_invoice_id", nullable = false)
    private DepartmentTaxInvoiceEntity invoice;

    @Column(name = "department_project_resource_requirement_id")
    private Long departmentProjectResourceRequirementId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "sac_hsn", nullable = false, length = 20)
    private String sacHsn;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "rate_per_month", nullable = false, precision = 14, scale = 2)
    private BigDecimal ratePerMonth;

    @Column(name = "duration_in_months", nullable = false)
    private Integer durationInMonths;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @PrePersist
    @PreUpdate
    void normalize() {
        description = trim(description);
        sacHsn = upper(trim(sacHsn));
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
