package com.maharecruitment.gov.in.department.entity;

import java.math.BigDecimal;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "department_proforma_invoice",
    indexes = {
        @Index(name = "idx_dep_pi_app_id", columnList = "department_project_application_id"),
        @Index(name = "idx_dep_pi_number", columnList = "pi_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentProformaInvoiceEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_proforma_invoice_id")
    private Long departmentProformaInvoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_project_application_id", nullable = false)
    private DepartmentProjectApplicationEntity application;

    @Column(name = "pi_number", nullable = false, unique = true, length = 50)
    private String piNumber;

    @Column(name = "base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}
