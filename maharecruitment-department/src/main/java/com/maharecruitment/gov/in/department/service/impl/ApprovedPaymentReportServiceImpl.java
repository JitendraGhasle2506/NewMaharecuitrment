package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.repository.DepartmentAdvancePaymentRepository;
import com.maharecruitment.gov.in.department.service.ApprovedPaymentReportService;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportFilter;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView;
import com.maharecruitment.gov.in.department.service.support.ApprovedPaymentReportFinancialYearResolver;
import com.maharecruitment.gov.in.department.service.support.ApprovedPaymentReportFinancialYearResolver.FinancialYearRange;

@Service
@Transactional(readOnly = true)
public class ApprovedPaymentReportServiceImpl implements ApprovedPaymentReportService {

    private static final DepartmentApplicationStatus APPROVED_STATUS = DepartmentApplicationStatus.AUDITOR_APPROVED;

    private final DepartmentAdvancePaymentRepository paymentRepository;

    public ApprovedPaymentReportServiceImpl(DepartmentAdvancePaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Page<ApprovedPaymentReportRowView> getApprovedPayments(ApprovedPaymentReportFilter filter, Pageable pageable) {
        NormalizedApprovedPaymentReportFilter normalizedFilter = normalizeFilter(filter);
        return paymentRepository.findApprovedPaymentReportRows(
                APPROVED_STATUS,
                normalizedFilter.departmentRegistrationId(),
                normalizedFilter.approvedFrom(),
                normalizedFilter.approvedToExclusive(),
                normalizedFilter.searchPattern(),
                pageable);
    }

    @Override
    public List<ApprovedPaymentReportRowView> getApprovedPaymentsForExport(ApprovedPaymentReportFilter filter) {
        NormalizedApprovedPaymentReportFilter normalizedFilter = normalizeFilter(filter);
        return paymentRepository.findApprovedPaymentReportRows(
                APPROVED_STATUS,
                normalizedFilter.departmentRegistrationId(),
                normalizedFilter.approvedFrom(),
                normalizedFilter.approvedToExclusive(),
                normalizedFilter.searchPattern());
    }

    @Override
    public ApprovedPaymentReportSummaryView getApprovedPaymentSummary(ApprovedPaymentReportFilter filter) {
        NormalizedApprovedPaymentReportFilter normalizedFilter = normalizeFilter(filter);
        return paymentRepository.summarizeApprovedPaymentReport(
                APPROVED_STATUS,
                normalizedFilter.departmentRegistrationId(),
                normalizedFilter.approvedFrom(),
                normalizedFilter.approvedToExclusive(),
                normalizedFilter.searchPattern());
    }

    @Override
    public List<DepartmentPaymentReportDepartmentOptionView> getDepartmentOptions() {
        return paymentRepository.findApprovedPaymentDepartments(APPROVED_STATUS).stream()
                .map(option -> new DepartmentPaymentReportDepartmentOptionView(
                        option.getDepartmentRegistrationId(),
                        hasText(option.getDepartmentName())
                                ? option.getDepartmentName().trim()
                                : "Department #" + option.getDepartmentRegistrationId()))
                .toList();
    }

    private NormalizedApprovedPaymentReportFilter normalizeFilter(ApprovedPaymentReportFilter filter) {
        ApprovedPaymentReportFilter safeFilter = filter == null ? new ApprovedPaymentReportFilter() : filter;

        FinancialYearRange financialYearRange = ApprovedPaymentReportFinancialYearResolver.resolve(
                safeFilter.getFinancialYear());

        LocalDate effectiveFromDate = safeFilter.getFromDate();
        if (financialYearRange != null && (effectiveFromDate == null
                || financialYearRange.startDate().isAfter(effectiveFromDate))) {
            effectiveFromDate = financialYearRange.startDate();
        }

        LocalDate effectiveToDate = safeFilter.getToDate();
        if (financialYearRange != null && (effectiveToDate == null
                || financialYearRange.endDate().isBefore(effectiveToDate))) {
            effectiveToDate = financialYearRange.endDate();
        }

        if (effectiveFromDate != null && effectiveToDate != null && effectiveFromDate.isAfter(effectiveToDate)) {
            LocalDate swap = effectiveFromDate;
            effectiveFromDate = effectiveToDate;
            effectiveToDate = swap;
        }

        return new NormalizedApprovedPaymentReportFilter(
                safeFilter.getDepartmentRegistrationId(),
                effectiveFromDate == null ? null : effectiveFromDate.atStartOfDay(),
                effectiveToDate == null ? null : effectiveToDate.plusDays(1).atStartOfDay(),
                buildSearchPattern(safeFilter.getSearchTerm()));
    }

    private String buildSearchPattern(String searchTerm) {
        if (!hasText(searchTerm)) {
            return null;
        }
        return "%" + searchTerm.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record NormalizedApprovedPaymentReportFilter(
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern) {
    }
}
