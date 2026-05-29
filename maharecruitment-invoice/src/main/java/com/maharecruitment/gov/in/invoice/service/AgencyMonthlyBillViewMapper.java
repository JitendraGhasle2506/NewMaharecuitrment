package com.maharecruitment.gov.in.invoice.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillLineItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillListItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEntity;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillLineItemEntity;

@Component
public class AgencyMonthlyBillViewMapper {

    public AgencyMonthlyBillListItemView toListItemView(AgencyMonthlyBillEntity entity) {
        return AgencyMonthlyBillListItemView.builder()
                .agencyMonthlyBillId(entity.getAgencyMonthlyBillId())
                .billNumber(entity.getBillNumber())
                .agencyId(entity.getAgencyId())
                .agencyName(entity.getAgencyName())
                .billYear(entity.getBillYear())
                .billMonth(entity.getBillMonth())
                .employeeType(entity.getEmployeeType())
                .generatedDate(entity.getGeneratedDate())
                .employeeCount(entity.getEmployeeCount())
                .attendanceAmount(entity.getAttendanceAmount())
                .agencyMarginAmount(entity.getAgencyMarginAmount())
                .totalAmount(entity.getTotalAmount())
                .build();
    }

    public AgencyMonthlyBillView toView(AgencyMonthlyBillEntity entity) {
        List<AgencyMonthlyBillLineItemView> lines = entity.getLineItems() == null
                ? List.of()
                : entity.getLineItems().stream()
                        .sorted(Comparator.comparing(AgencyMonthlyBillLineItemEntity::getLineNumber,
                                Comparator.nullsLast(Integer::compareTo)))
                        .map(this::toLineItemView)
                        .toList();

        return AgencyMonthlyBillView.builder()
                .agencyMonthlyBillId(entity.getAgencyMonthlyBillId())
                .billNumber(entity.getBillNumber())
                .agencyId(entity.getAgencyId())
                .agencyName(entity.getAgencyName())
                .billYear(entity.getBillYear())
                .billMonth(entity.getBillMonth())
                .employeeType(entity.getEmployeeType())
                .generatedDate(entity.getGeneratedDate())
                .periodFrom(entity.getPeriodFrom())
                .periodTo(entity.getPeriodTo())
                .daysInMonth(entity.getDaysInMonth())
                .employeeCount(entity.getEmployeeCount())
                .agencyMarginRate(entity.getAgencyMarginRate())
                .attendanceAmount(entity.getAttendanceAmount())
                .agencyMarginAmount(entity.getAgencyMarginAmount())
                .totalAmount(entity.getTotalAmount())
                .createdDate(entity.getCreatedDate())
                .createdBy(entity.getCreatedBy())
                .lineItems(lines)
                .build();
    }

    private AgencyMonthlyBillLineItemView toLineItemView(AgencyMonthlyBillLineItemEntity entity) {
        return AgencyMonthlyBillLineItemView.builder()
                .lineNumber(entity.getLineNumber())
                .employeeId(entity.getEmployeeId())
                .employeeCode(entity.getEmployeeCode())
                .requestId(entity.getRequestId())
                .employeeName(entity.getEmployeeName())
                .employeeType(entity.getEmployeeType())
                .designationId(entity.getDesignationId())
                .designationName(entity.getDesignationName())
                .levelCode(entity.getLevelCode())
                .monthlyRate(entity.getMonthlyRate())
                .daysInMonth(entity.getDaysInMonth())
                .payableDays(entity.getPayableDays())
                .presentDays(entity.getPresentDays())
                .absentDays(entity.getAbsentDays())
                .leaveDays(entity.getLeaveDays())
                .compOffDays(entity.getCompOffDays())
                .tourDays(entity.getTourDays())
                .holidayDays(entity.getHolidayDays())
                .weekOffDays(entity.getWeekOffDays())
                .attendanceAmount(entity.getAttendanceAmount())
                .agencyMarginRate(entity.getAgencyMarginRate())
                .agencyMarginAmount(entity.getAgencyMarginAmount())
                .lineTotal(entity.getLineTotal())
                .build();
    }
}
