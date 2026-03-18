package com.maharecruitment.gov.in.department.entity;

import java.math.BigDecimal;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "department_advance_payment", indexes = {
        @Index(name = "idx_dep_adv_pay_app", columnList = "department_project_application_id"),
        @Index(name = "idx_dep_adv_pay_status", columnList = "application_status"),
        @Index(name = "idx_dep_adv_pay_receipt", columnList = "receipt_number")
})
@Getter
@Setter
@NoArgsConstructor
public class DepartmentAdvancePaymentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_project_application_id", nullable = false)
    private DepartmentProjectApplicationEntity application;

    @Column(name = "department_registration_id", nullable = false)
    private Long departmentRegistrationId;

    @Column(name = "proforma_invoice_id")
    private String proformaInvoiceId;

    @Column(name = "receipt_number", nullable = false, length = 100)
    private String receiptNumber;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "receipt_file_path", length = 500)
    private String receiptFilePath;

    @Column(name = "receipt_original_name", length = 255)
    private String receiptOriginalName;

    @Column(name = "receipt_file_type", length = 120)
    private String receiptFileType;

    @Column(name = "receipt_file_size")
    private Long receiptFileSize;

    @Convert(converter = DepartmentApplicationStatusConverter.class)
    @Column(name = "application_status", nullable = false, length = 30)
    private DepartmentApplicationStatus applicationStatus = DepartmentApplicationStatus.DRAFT;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "utr_number", length = 100)
    private String utrNumber;
}
