package com.maharecruitment.gov.in.invoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

@Getter
@Setter
@Entity
@Table(
        name = "agency_monthly_bill",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agency_monthly_bill_number", columnNames = "bill_number"),
                @UniqueConstraint(name = "uk_agency_monthly_bill_period", columnNames = {
                        "agency_id", "bill_year", "bill_month", "employee_type" })
        },
        indexes = {
                @Index(name = "idx_agency_monthly_bill_agency", columnList = "agency_id"),
                @Index(name = "idx_agency_monthly_bill_period", columnList = "bill_year,bill_month"),
                @Index(name = "idx_agency_monthly_bill_generated_date", columnList = "generated_date")
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMonthlyBillEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_monthly_bill_id")
    private Long agencyMonthlyBillId;

    @Column(name = "bill_number", nullable = false, length = 80)
    private String billNumber;

    @Column(name = "agency_id", nullable = false)
    private Long agencyId;

    @Column(name = "agency_name", nullable = false, length = 200)
    private String agencyName;

    @Column(name = "bill_year", nullable = false)
    private Integer billYear;

    @Column(name = "bill_month", nullable = false)
    private Integer billMonth;

    @Builder.Default
    @Column(name = "employee_type", nullable = false, length = 20)
    private String employeeType = AgencyMonthlyBillEmployeeType.ALL.name();

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "days_in_month", nullable = false)
    private Integer daysInMonth;

    @Column(name = "employee_count", nullable = false)
    private Integer employeeCount;

    @Column(name = "agency_margin_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal agencyMarginRate;

    @Column(name = "attendance_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal attendanceAmount;

    @Column(name = "agency_margin_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal agencyMarginAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<AgencyMonthlyBillLineItemEntity> lineItems = new ArrayList<>();

    public void replaceLineItems(List<AgencyMonthlyBillLineItemEntity> items) {
        lineItems.clear();
        if (items == null) {
            return;
        }
        for (AgencyMonthlyBillLineItemEntity item : items) {
            addLineItem(item);
        }
    }

    public void addLineItem(AgencyMonthlyBillLineItemEntity item) {
        if (item == null) {
            return;
        }
        item.setBill(this);
        lineItems.add(item);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        billNumber = trim(billNumber);
        agencyName = trim(agencyName);
        employeeType = normalizeEmployeeType(employeeType);
        active = !Boolean.FALSE.equals(active);
    }

    private String normalizeEmployeeType(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return AgencyMonthlyBillEmployeeType.ALL.name();
        }
        try {
            return AgencyMonthlyBillEmployeeType.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return AgencyMonthlyBillEmployeeType.ALL.name();
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
