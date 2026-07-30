package com.maharecruitment.gov.in.invoice.entity;

import java.math.BigDecimal;

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

@Getter
@Setter
@Entity
@Table(
        name = "agency_monthly_bill_line_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agency_monthly_bill_line_row",
                        columnNames = { "agency_monthly_bill_id", "line_no" })
        },
        indexes = {
                @Index(name = "idx_agency_monthly_bill_line_bill", columnList = "agency_monthly_bill_id"),
                @Index(name = "idx_agency_monthly_bill_line_employee", columnList = "employee_id")
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMonthlyBillLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_monthly_bill_line_item_id")
    private Long agencyMonthlyBillLineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_monthly_bill_id", nullable = false)
    private AgencyMonthlyBillEntity bill;

    @Column(name = "line_no", nullable = false)
    private Integer lineNumber;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "employee_name", nullable = false, length = 150)
    private String employeeName;

    @Column(name = "employee_type", nullable = false, length = 20)
    private String employeeType;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "designation_name", nullable = false, length = 200)
    private String designationName;

    @Column(name = "level_code", length = 50)
    private String levelCode;

    @Column(name = "monthly_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "days_in_month", nullable = false)
    private Integer daysInMonth;

    @Column(name = "payable_days", nullable = false)
    private Long payableDays;

    @Column(name = "present_days", nullable = false)
    private Long presentDays;

    @Column(name = "absent_days", nullable = false)
    private Long absentDays;

    @Column(name = "leave_days", nullable = false)
    private Long leaveDays;

    @Column(name = "comp_off_days", nullable = false)
    private Long compOffDays;

    @Column(name = "tour_days", nullable = false)
    private Long tourDays;

    @Column(name = "holiday_days", nullable = false)
    private Long holidayDays;

    @Column(name = "week_off_days", nullable = false)
    private Long weekOffDays;

    @Column(name = "attendance_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal attendanceAmount;

    @Column(name = "agency_margin_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal agencyMarginRate;

    @Column(name = "agency_margin_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal agencyMarginAmount;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @PrePersist
    @PreUpdate
    void normalize() {
        employeeCode = trim(employeeCode);
        requestId = trim(requestId);
        employeeName = trim(employeeName);
        employeeType = normalizeEmployeeType(employeeType);
        designationName = trim(designationName);
        levelCode = trim(levelCode);
    }

    private String normalizeEmployeeType(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return AgencyMonthlyBillEmployeeType.EXTERNAL.name();
        }
        try {
            return AgencyMonthlyBillEmployeeType.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return AgencyMonthlyBillEmployeeType.EXTERNAL.name();
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
