package com.maharecruitment.gov.in.department.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView;

public interface DepartmentAdvancePaymentReportRepository {

    Page<ApprovedPaymentReportRowView> findApprovedPaymentReportRows(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern,
            Pageable pageable);

    List<ApprovedPaymentReportRowView> findApprovedPaymentReportRows(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern);

    ApprovedPaymentReportSummaryView summarizeApprovedPaymentReport(
            DepartmentApplicationStatus approvedStatus,
            Long departmentRegistrationId,
            LocalDateTime approvedFrom,
            LocalDateTime approvedToExclusive,
            String searchPattern);

    List<DepartmentPaymentReportDepartmentOptionView> findApprovedPaymentDepartments(
            DepartmentApplicationStatus approvedStatus);
}
