package com.maharecruitment.gov.in.invoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
        name = "department_tax_invoice",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dep_tax_invoice_request_id", columnNames = "request_id"),
                @UniqueConstraint(name = "uk_dep_tax_invoice_ti_number", columnNames = "ti_number"),
                @UniqueConstraint(name = "uk_dep_tax_invoice_application_id",
                        columnNames = "department_project_application_id")
        },
        indexes = {
                @Index(name = "idx_dep_tax_invoice_request_id", columnList = "request_id"),
                @Index(name = "idx_dep_tax_invoice_ti_number", columnList = "ti_number"),
                @Index(name = "idx_dep_tax_invoice_application_id", columnList = "department_project_application_id"),
                @Index(name = "idx_dep_tax_invoice_registration_id", columnList = "department_registration_id"),
                @Index(name = "idx_dep_tax_invoice_ti_date", columnList = "ti_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentTaxInvoiceEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_tax_invoice_id")
    private Long departmentTaxInvoiceId;

    @Column(name = "department_project_application_id", nullable = false)
    private Long departmentProjectApplicationId;

    @Column(name = "department_registration_id", nullable = false)
    private Long departmentRegistrationId;

    @Column(name = "request_id", nullable = false, length = 32)
    private String requestId;

    @Column(name = "ti_number", nullable = false, length = 50)
    private String tiNumber;

    @Column(name = "ti_date", nullable = false)
    private LocalDate tiDate;

    @Column(name = "dept_ref_date", nullable = false)
    private LocalDate deptRefDate;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "project_code", length = 100)
    private String projectCode;

    @Column(name = "pm_name", length = 100)
    private String pmName;

    @Column(name = "billed_to", nullable = false, length = 200)
    private String billedTo;

    @Column(name = "billing_address", nullable = false, length = 1000)
    private String billingAddress;

    @Builder.Default
    @Column(name = "client_gstin_available", nullable = false)
    private Boolean clientGstinAvailable = Boolean.FALSE;

    @Column(name = "client_gst_number", length = 15)
    private String clientGstNumber;

    @Column(name = "place_of_supply", nullable = false, length = 100)
    private String placeOfSupply;

    @Column(name = "base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "cgst_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal cgstRate;

    @Column(name = "cgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal sgstRate;

    @Column(name = "sgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_address", nullable = false, length = 1000)
    private String companyAddress;

    @Column(name = "cin_number", nullable = false, length = 21)
    private String cinNumber;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "gst_number", nullable = false, length = 15)
    private String gstNumber;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "amount_in_words", nullable = false, length = 500)
    private String amountInWords;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<DepartmentTaxInvoiceLineItemEntity> lineItems = new ArrayList<>();

    public void replaceLineItems(List<DepartmentTaxInvoiceLineItemEntity> items) {
        lineItems.clear();
        if (items == null) {
            return;
        }
        for (DepartmentTaxInvoiceLineItemEntity item : items) {
            addLineItem(item);
        }
    }

    public void addLineItem(DepartmentTaxInvoiceLineItemEntity item) {
        if (item == null) {
            return;
        }
        item.setInvoice(this);
        lineItems.add(item);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        requestId = trim(requestId);
        tiNumber = trim(tiNumber);
        projectName = trim(projectName);
        projectCode = trim(projectCode);
        pmName = trim(pmName);
        billedTo = trim(billedTo);
        billingAddress = trim(billingAddress);
        clientGstNumber = upper(trim(clientGstNumber));
        placeOfSupply = trim(placeOfSupply);
        companyName = trim(companyName);
        companyAddress = trim(companyAddress);
        cinNumber = upper(trim(cinNumber));
        panNumber = upper(trim(panNumber));
        gstNumber = upper(trim(gstNumber));
        bankName = trim(bankName);
        branchName = trim(branchName);
        accountHolderName = trim(accountHolderName);
        accountNumber = trim(accountNumber);
        ifscCode = upper(trim(ifscCode));
        amountInWords = trim(amountInWords);

        clientGstinAvailable = Boolean.TRUE.equals(clientGstinAvailable);
        active = !Boolean.FALSE.equals(active);
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
